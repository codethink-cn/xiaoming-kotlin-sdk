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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.event.AbstractEvent
import cn.codethink.xiaoming.event.ContextAwareEvent
import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.event.EventContext
import cn.codethink.xiaoming.event.EventPolicy
import cn.codethink.xiaoming.event.EventState
import cn.codethink.xiaoming.event.LocalEventManager
import cn.codethink.xiaoming.event.trace.EventTrace
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class LocalEventManagerImpl(
    override val platform: LocalPlatform
) : LocalEventManager {
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

    override suspend fun <E : Event> publishLocalEvent(event: E, policy: EventPolicy): EventContext<E> {
        TODO("Not yet implemented")
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <E : Event> publishEvent(event: E, policy: EventPolicy): EventContext<E> {
        require(event is AbstractEvent) { "Event must be an instance of AbstractEvent" }

        val context = EventContextImpl(event, policy)
        if (event is ContextAwareEvent<*>) {
            event.applyEventContext(context as EventContext<Nothing>)
        }

        // TODO: publish event.

        return context
    }
}