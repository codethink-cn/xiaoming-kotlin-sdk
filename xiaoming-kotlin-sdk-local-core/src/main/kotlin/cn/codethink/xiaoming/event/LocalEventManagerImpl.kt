/*
 * Copyright 2025 CodeThink Technologies and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.event.listener.Listener
import cn.codethink.xiaoming.event.listener.ListenerConfiguration
import cn.codethink.xiaoming.event.listener.ListenerDescriptor
import cn.codethink.xiaoming.event.listener.ListenerDescriptorImpl
import cn.codethink.xiaoming.event.listener.ListenerHandler
import cn.codethink.xiaoming.event.listener.ListenerPriority
import cn.codethink.xiaoming.event.trace.EventTrace
import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableDirectedAcyclicGraphImpl
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

@OptIn(InternalApi::class)
@Suppress("UNCHECKED_CAST")
class LocalEventManagerImpl(
    override val platform: LocalPlatform
) : LocalEventManager {
    private val logger = KotlinLogging.logger("${LocalEventManager::class}")

    private inner class EventContextImpl<E : Event>(
        override val event: E,
        override val policy: EventPolicy
    ) : EventContext<E> {
        override val platform: Platform = this@LocalEventManagerImpl.platform

        private val mutableState = AtomicReference<EventState>(EventState.ALLOCATED)
        override val state: EventState get() = mutableState.get()

        private val mutableTraces = CopyOnWriteArrayList<EventTrace<E>>()
        override val traces: List<EventTrace<E>> get() = mutableTraces.toList()
    }

    private val eventTypesCache = ConcurrentHashMap<KClass<out Event>, List<Pair<String, KClass<*>>>>()

    private val eventTypeHintsRegisteredClasses = ConcurrentHashMap.newKeySet<KClass<out Event>>()

    private fun getEventTypes(type: KClass<out Event>): List<Pair<String, KClass<*>>> {
        return eventTypesCache.computeIfAbsent(type) { type.collectEventTypes() }
    }

    private fun ensureEventTypeHintsRegistered(type: KClass<out Event>, operation: Operation, eventTypes: List<Pair<String, KClass<*>>>) {
        if (eventTypeHintsRegisteredClasses.add(type)) {
            for (eventType in eventTypes) {
                platform.serializationManager.codecResolver.registerNameBasedTypeHint(
                    type = Event::class.java,
                    nameField = FIELD_TYPE,
                    name = eventType.first,
                    hint = eventType.second.java as Class<Event>,
                    operation = operation
                )
            }
        }
    }

    // 一旦使用监听器，需要获取信号量。
    // 每次注册修改监听器就重新构造一次依赖图。
    private val mutex = Mutex()

    private val listeners = mutableMapOf<NamespaceId, ListenerImpl<*>>()
    private var listenerDependencyGraph = MutableDirectedAcyclicGraphImpl<ListenerImpl<*>, Unit>()

    private inner class ListenerImpl<E : Event>(
        override val descriptor: ListenerDescriptor,
        override val type: String,
        override var handler: ListenerHandler<E>,
        override val configuration: ListenerConfiguration,
        private val operation: Operation
    ) : Listener<E> {
        suspend fun safeListen(context: EventContext<E>) {
            try {
                handler.listen(context.event, context)
            } catch (t: Throwable) {
                logger.error(t) {
                    "Exception thrown while listening to event ${context.event.description} (${context.event}) " +
                            "by listener ${descriptor.id} registered by ${operation.operator}"
                }
            }
        }
    }

    private inner class ListenerRegistration<E : Event>(
        override val value: Listener<E>,
        override val operation: Operation
    ) : MutableRegistration<Listener<E>> {
        override val isRemoved: Boolean get() = listeners[value.descriptor.id] !== value

        override fun remove() {
            check(tryRemove()) { "Listener ${value.descriptor.id} is already removed" }
        }

        override fun tryRemove(): Boolean {
            return runBlocking {
                mutex.withLock {
                    val nowListener = listeners[value.descriptor.id]
                    if (nowListener !== value) {
                        return@withLock false
                    }

                    listeners.remove(value.descriptor.id)
                    rebuildListenerDependencyGraphNoMutex()
                    true
                }
            }
        }
    }

    private val priorities = listOf(
        ListenerPriority.HIGHEST, ListenerPriority.HIGH,
        ListenerPriority.DEFAULT,
        ListenerPriority.LOW, ListenerPriority.LOWEST
    )

    private class ListenerContext(val listener: ListenerImpl<*>) {
        enum class State {
            NOT_LISTENED_YET,
            LISTENING,
            LISTENED
        }

        private val atomicState = AtomicReference<State>(State.NOT_LISTENED_YET)

        fun trySetListening(): Boolean {
            return atomicState.compareAndSet(State.NOT_LISTENED_YET, State.LISTENING)
        }

        fun isListened(): Boolean {
            return atomicState.get() == State.LISTENED
        }

        fun setListened() {
            atomicState.set(State.LISTENED)
        }
    }

    override suspend fun <E : Event> publishEvent(event: E, operation: Operation, policy: EventPolicy): EventContext<E> {
        require(event is AbstractEvent) { "Event must be an instance of AbstractEvent" }

        val context = EventContextImpl(event, policy)
        if (event is ContextAwareEvent<*>) {
            event.applyEventContext(context as EventContext<Nothing>)
        }

        val eventTypes = getEventTypes(event::class)

        require(eventTypes.isNotEmpty()) { "Event type must be declared, but ${event::class} has no @EventType" }
        ensureEventTypeHintsRegistered(event::class, operation, eventTypes)

        val eventTypeStrings = eventTypes.map { it.first }
        logger.trace { "Publishing event ${event.description} ($event) with types $eventTypeStrings" }

        mutex.withLock {
            val listenerContexts = listeners.values.map { ListenerContext(it) }.associateBy { it.listener }
            val listenedListenerCount = AtomicInteger(0)

            while (listenedListenerCount.get() < listenerContexts.size) {
                for (priority in priorities) {
                    listenerDependencyGraph.forEachConcurrently {
                        val listenerContext = listenerContexts[it.value] ?: return@forEachConcurrently
                        if (listenerContext.isListened() || it.value.configuration.priority != priority) {
                            return@forEachConcurrently
                        }
                        if (it.value.type !in eventTypeStrings) {
                            listenedListenerCount.incrementAndGet()
                            return@forEachConcurrently
                        }

                        val allPredecessorsListened = it.directPredecessors
                            .mapNotNull { pre -> listenerContexts[pre.value] }
                            .all { pre -> pre.isListened() }

                        if (!allPredecessorsListened) {
                            return@forEachConcurrently
                        }

                        if (listenerContext.trySetListening()) {
                            it.value.safeListen(context as EventContext<Nothing>)
                            listenerContext.setListened()
                            listenedListenerCount.incrementAndGet()
                        }
                    }
                }
            }
        }

        return context
    }

    private fun rebuildListenerDependencyGraphNoMutex() {
        listenerDependencyGraph = MutableDirectedAcyclicGraphImpl()

        val listenerNodes = listeners.values
            .map { listenerDependencyGraph.allocate(it) }
            .associateBy { it.value.descriptor }

        for (listener in listeners.values) {
            val listenerNode = listenerNodes[listener.descriptor] ?: continue

            for (before in listener.configuration.before) {
                val beforeNode = listenerNodes[before] ?: continue
                listenerDependencyGraph.link(beforeNode, listenerNode, Unit)
            }

            for (after in listener.configuration.after) {
                val afterNode = listenerNodes[after] ?: continue
                listenerDependencyGraph.link(listenerNode, afterNode, Unit)
            }
        }
    }

    override suspend fun <E : Event> registerListener(
        id: NamespaceId,
        type: Class<E>,
        operation: Operation,
        configuration: ListenerConfiguration,
        handler: ListenerHandler<E>
    ): MutableRegistration<Listener<E>> {
        val eventTypes = getEventTypes(type.kotlin)

        require(eventTypes.isNotEmpty()) { "Event type must be declared, but ${type.kotlin} has no @EventType" }
        ensureEventTypeHintsRegistered(type.kotlin, operation, eventTypes)

        val (eventType, eventTypeClass) = eventTypes.first()
        require(eventTypeClass == type.kotlin) {
            "Event type must have a type declaration, but ${type.kotlin} has no @EventType and its can not be inferred from the class"
        }

        return registerListener(
            id = id,
            type = eventType,
            operation = operation,
            configuration = configuration,
            handler = handler as ListenerHandler<Event>
        ) as MutableRegistration<Listener<E>>
    }

    override suspend fun registerListener(
        id: NamespaceId,
        type: String,
        operation: Operation,
        configuration: ListenerConfiguration,
        handler: ListenerHandler<Event>
    ): MutableRegistration<Listener<Event>> {
        return mutex.withLock {
            val listener = ListenerImpl(
                descriptor = ListenerDescriptorImpl(id),
                type = type,
                handler = handler,
                configuration = configuration,
                operation = operation
            )

            listeners[id] = listener
            rebuildListenerDependencyGraphNoMutex()

            ListenerRegistration(listener, operation)
        }
    }

    override suspend fun registerListeners(listeners: Any, operation: Operation): List<MutableRegistration<Listener<Event>>> {
        val results = mutableListOf<MutableRegistration<Listener<Event>>>()

        val listenersClass = listeners::class
//        for (memberFunction in listenersClass.memberFunctions) {
//
//        }

        return emptyList()
    }
}