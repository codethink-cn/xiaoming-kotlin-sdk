/*
 * Copyright 2024 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.DefaultListRegistrations
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Registrations
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.tristateOf
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.permission.data.PermissionProfile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * The core permission service api.
 *
 * @author Chuanwise
 * @see LocalPermissionService
 */
class LocalPermissionServiceApi(
    val api: LocalPlatformInternalApi
) {
    val logger by api::logger

    /**
     * Calculator is used to deal with different type of subjects.
     */
    private val permissionCalculators = DefaultStringMapRegistrations<PermissionCalculator<*>>()

    /**
     * The API will call the matched permission comparators, and use them to compare record
     * and the given. If not found, use the [defaultPermissionComparators].
     */
    val permissionComparators = PermissionComparatorRegistrations()
    val defaultPermissionComparators = DefaultListRegistrations<PermissionComparator>()

    /**
     * Check if permission record is valid, usually called in [PermissionComparator].
     *
     * @see DefaultPermissionComparator
     */
    val permissionContextMatchers = DefaultStringMapRegistrations<Matcher<*>>()

    val permissionMetas = PermissionMetaRegistrations()

    val permissionAddingCheckers = PermissionAddingCheckerRegistrations()

    init {
        registerDefaultPermissionComparator(DefaultPermissionComparator, XiaomingSdkSubject)
    }

    fun setPermission(
        profile: PermissionProfile,
        subjectMatcher: Matcher<Subject>,
        nodeMatcher: Matcher<SegmentId>,
        value: Boolean?,
        argumentMatchers: Map<String, Matcher<*>> = emptyMap(),
        context: Map<String, Any?> = emptyMap(),
        caller: Subject? = null, cause: Cause? = null
    ) {
        // Check if operation valid.
        val checkers = permissionAddingCheckers[nodeMatcher]
        if (checkers.isNotEmpty()) {
            val addingContext = PermissionSettingContext(
                this, profile, subjectMatcher, nodeMatcher, value, argumentMatchers, context, caller, cause
            )
            checkers.forEach { checker ->
                checker.value.check(addingContext)
            }
        }

        // Do operation.
        val records = api.data.permissionRecords.getRecords(profile)

        // If same record exist, print warning.
        val almostSameRecord = records.firstOrNull {
            it.subjectMatcher == subjectMatcher &&
                    it.nodeMatcher == nodeMatcher && it.argumentMatchers == argumentMatchers && it.context == context
        }
        if (almostSameRecord != null) {
            if (almostSameRecord.value == value) {
                api.logger.warn { "Same permission record already exist ($almostSameRecord), no any effect." }
            } else {
                // Change its value.
                almostSameRecord.value = value
            }
            return
        }

        // Add new record.
        api.data.permissionRecords.addRecord(
            profile, subjectMatcher, nodeMatcher, value, argumentMatchers
        )
    }

    fun hasPermission(
        profile: PermissionProfile, permission: Permission,
        context: Map<String, Any?> = emptyMap(), caller: Subject? = null, cause: Cause? = null
    ): Boolean? {
        api.data.permissionRecords.getRecords(profile).forEach {
            val comparingContext = PermissionComparingContext(
                this, profile, permission, it, context, caller, cause
            )
            var comparatorRegistrations: List<Registration<PermissionComparator>> =
                permissionComparators[permission.descriptor.node]

            if (comparatorRegistrations.isEmpty()) {
                comparatorRegistrations = defaultPermissionComparators.toList()
            }

            // null means no comparator matched.
            var result: Tristate? = null
            var resultRegistration: Registration<PermissionComparator>? = null
            for (registration in comparatorRegistrations) {
                if (registration.value.compare(comparingContext)) {
                    result = tristateOf(it.value)
                    resultRegistration = registration
                    break
                }
            }

            if (result != null) {
                return result.value
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun hasPermission(
        subject: Subject, permission: Permission,
        context: Map<String, Any?> = emptyMap(), caller: Subject? = null, cause: Cause? = null
    ): Boolean? {
        val calculator = permissionCalculators[subject.type]?.value as PermissionCalculator<Subject>? ?: return null
        val calculatingContext = PermissionCalculatingContext(this, subject, permission, context, caller, cause)

        return calculator.hasPermission(calculatingContext)
    }

    fun registerPermissionCalculator(
        type: String, calculator: PermissionCalculator<*>, subject: Subject
    ) = permissionCalculators.register(type, DefaultRegistration(calculator, subject))

    fun unregisterPermissionCalculatorByType(type: String) = permissionCalculators.unregisterByKey(type)
    fun unregisterPermissionCalculatorBySubject(subject: Subject) = permissionCalculators.unregisterBySubject(subject)

    fun registerPermissionComparator(
        matcher: Matcher<SegmentId>, comparator: PermissionComparator, subject: Subject
    ) = permissionComparators.register(matcher, comparator, subject)

    fun unregisterPermissionComparatorBySubject(subject: Subject) = permissionComparators.unregisterBySubject(subject)
    fun unregisterPermissionComparatorIfMatched(id: SegmentId) = permissionComparators.unregisterIfMatched(id)

    fun registerPermissionMeta(id: SegmentId, meta: PermissionMeta, subject: Subject) =
        permissionMetas.register(id, meta, subject)

    fun unregisterPermissionMetaById(id: SegmentId) = permissionMetas.unregisterById(id)
    fun unregisterPermissionMetaBySubject(subject: Subject) = permissionMetas.unregisterBySubject(subject)
    fun unregisterPermissionMetaByIdAndSubject(id: SegmentId, subject: Subject) =
        permissionMetas.unregisterByIdAndSubject(id, subject)

    fun registerDefaultPermissionComparator(comparator: PermissionComparator, subject: Subject): Unit =
        defaultPermissionComparators.register(
            DefaultRegistration(comparator, subject)
        )

    fun unregisterDefaultPermissionComparatorByComparator(comparator: PermissionComparator) =
        defaultPermissionComparators.unregisterByValue(comparator)

    fun unregisterDefaultPermissionComparatorBySubject(subject: Subject) =
        defaultPermissionComparators.unregisterBySubject(subject)

    fun unregisterBySubject(subject: Subject) {
        unregisterPermissionCalculatorBySubject(subject)
        unregisterPermissionComparatorBySubject(subject)
        unregisterPermissionMetaBySubject(subject)
        unregisterDefaultPermissionComparatorBySubject(subject)
    }
}


class PermissionComparatorRegistrations : Registrations {
    data class PermissionComparatorRegistration(
        val matcher: Matcher<SegmentId>,
        override val value: PermissionComparator,
        override val subject: Subject
    ) : Registration<PermissionComparator>

    private val comparators = CopyOnWriteArrayList<PermissionComparatorRegistration>()
    operator fun get(id: SegmentId) = comparators.filter { it.matcher.isMatched(id) }

    fun register(matcher: Matcher<SegmentId>, comparator: PermissionComparator, subject: Subject) {
        comparators.add(PermissionComparatorRegistration(matcher, comparator, subject))
    }

    override fun unregisterBySubject(subject: Subject): Boolean = comparators.removeAll { it.subject == subject }
    fun unregisterIfMatched(id: SegmentId): Boolean = comparators.removeAll { it.matcher.isMatched(id) }
}


class PermissionAddingCheckerRegistrations : Registrations {
    data class PermissionAddingCheckerRegistration(
        val node: SegmentId,
        override val value: PermissionSettingChecker,
        override val subject: Subject
    ) : Registration<PermissionSettingChecker>

    private val checkers = CopyOnWriteArrayList<PermissionAddingCheckerRegistration>()
    operator fun get(matcher: Matcher<SegmentId>) = checkers.filter { matcher.isMatched(it.node) }

    fun register(node: SegmentId, checker: PermissionSettingChecker, subject: Subject) {
        checkers.add(PermissionAddingCheckerRegistration(node, checker, subject))
    }

    override fun unregisterBySubject(subject: Subject): Boolean = checkers.removeAll { it.subject == subject }
    fun unregisterIfMatched(matcher: Matcher<SegmentId>): Boolean = checkers.removeAll { matcher.isMatched(it.node) }
}


class PermissionMetaRegistrations : Registrations {
    class PermissionMetaRegistration(
        val id: SegmentId,
        override val value: PermissionMeta,
        override val subject: Subject
    ) : Registration<PermissionMeta>

    private val lock = ReentrantReadWriteLock()

    private val metaBySegmentId = mutableMapOf<SegmentId, MutableMap<Subject, PermissionMetaRegistration>>()
    private val metaBySubject = mutableMapOf<Subject, MutableMap<SegmentId, PermissionMetaRegistration>>()

    operator fun get(id: SegmentId): Map<Subject, PermissionMetaRegistration>? = lock.read {
        metaBySegmentId[id]
    }

    operator fun get(subject: Subject): Map<SegmentId, PermissionMetaRegistration>? = lock.read {
        metaBySubject[subject]
    }

    operator fun get(id: SegmentId, subject: Subject): PermissionMetaRegistration? = lock.read {
        metaBySegmentId[id]?.get(subject)
    }

    fun register(id: SegmentId, meta: PermissionMeta, subject: Subject) {
        val registration = PermissionMetaRegistration(id, meta, subject)

        lock.write {
            metaBySegmentId.getOrPut(id) { mutableMapOf() }[subject] = registration
            metaBySubject.getOrPut(subject) { mutableMapOf() }[id] = registration
        }
    }

    override fun unregisterBySubject(subject: Subject): Boolean = lock.write {
        val registrations = metaBySubject.remove(subject) ?: return false
        registrations.values.forEach { registration ->
            metaBySegmentId[registration.id]?.remove(subject)
        }
        return true
    }

    fun unregisterById(id: SegmentId): Boolean = lock.write {
        val registrations = metaBySegmentId.remove(id) ?: return false
        registrations.values.forEach { registration ->
            metaBySubject[registration.subject]?.remove(id)
        }
        return true
    }

    fun unregisterByIdAndSubject(id: SegmentId, subject: Subject): Boolean = lock.write {
        val registration = metaBySegmentId[id]?.remove(subject) ?: return false
        metaBySubject[subject]?.remove(id)
        return true
    }
}