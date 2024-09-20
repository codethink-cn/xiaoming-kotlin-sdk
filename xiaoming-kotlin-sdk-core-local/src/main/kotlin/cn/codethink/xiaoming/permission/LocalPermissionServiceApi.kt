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
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Registrations
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingSdkSubject
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
    val internalApi: LocalPlatformInternalApi
) {
    val logger by internalApi::logger

    /**
     * Calculator is used to deal with different type of subjects.
     */
    val permissionCalculatorRegistrations = DefaultStringMapRegistrations<PermissionCalculator<*>>()

    /**
     * Check if permission record is valid, usually called in [PermissionComparator].
     *
     * @see DefaultPermissionComparator
     */
    val permissionContextMatcherRegistrations = DefaultStringMapRegistrations<Matcher<*>>()

    val permissionMetaRegistrations = PermissionMetaRegistrations()

    val permissionSettingCheckerRegistrations = DefaultStringMapRegistrations<PermissionSettingChecker>()

    init {
        registerPermissionSettingChecker(
            PERMISSION_COMPARATOR_TYPE_INHERITANCE, InheritancePermissionSettingChecker, XiaomingSdkSubject
        )
    }

    fun setPermission(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        contextMatchers: Map<String, Matcher<Any?>> = emptyMap(),
        caller: Subject? = null, cause: Cause? = null
    ) {
        // Check if operation valid.
        permissionSettingCheckerRegistrations[comparator.type]?.let {
            val addingContext = PermissionSettingContext(this, profile, comparator, contextMatchers, caller, cause)
            it.value.check(addingContext)
        }

        // Do operation.
        val records = internalApi.data.permissionRecordData.getRecordsByProfileId(profile.id)

        val previousRecords = records.filter { it.comparator == comparator && it.contextMatchers == contextMatchers }
        if (previousRecords.isNotEmpty()) {
            if (previousRecords.size == 1) {
                val thatOne = previousRecords.first()
                logger.warn { "Permission record already set: $thatOne." }
                return
            }
            internalApi.data.permissionRecordData.delete(previousRecords)
        }

        // Add new record.
        internalApi.data.permissionRecordData.insert(profile, comparator, contextMatchers)
    }

    fun unsetPermission(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        contextMatchers: Map<String, Matcher<Any?>> = emptyMap(),
        caller: Subject? = null, cause: Cause? = null
    ): Boolean {
        val records = internalApi.data.permissionRecordData.getRecordsByProfileId(profile.id)
            .filter { it.comparator == comparator && it.contextMatchers == contextMatchers }

        if (records.isNotEmpty()) {
            internalApi.data.permissionRecordData.delete(records)
        }

        return records.isNotEmpty()
    }

    fun hasPermission(
        profileId: Long, permission: Permission,
        context: Map<String, Any?> = emptyMap(),
        caller: Subject? = null, cause: Cause? = null
    ): Boolean? {
        internalApi.data.permissionRecordData.getRecordsByProfileId(profileId).forEach {
            val comparingContext = PermissionComparingContext(
                this, profileId, permission, it, context, caller, cause
            )
            it.comparator.compare(comparingContext)?.let { result -> return result.value }
        }
        return null
    }

    fun hasPermission(
        profile: PermissionProfile, permission: Permission,
        context: Map<String, Any?> = emptyMap(),
        caller: Subject? = null, cause: Cause? = null
    ): Boolean? {
        internalApi.data.permissionRecordData.getRecordsByProfileId(profile.id).forEach {
            val comparingContext = PermissionComparingContext(
                this, profile.id, permission, it, context, caller, cause
            )
            it.comparator.compare(comparingContext)?.let { result -> return result.value }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun hasPermission(
        subject: Subject, permission: Permission,
        context: Map<String, Any?> = emptyMap(), caller: Subject? = null, cause: Cause? = null
    ): Boolean? {
        val calculator =
            permissionCalculatorRegistrations[subject.type]?.value as PermissionCalculator<Subject>? ?: return null
        val calculatingContext = PermissionCalculatingContext(this, subject, permission, context, caller, cause)

        return calculator.hasPermission(calculatingContext)
    }

    fun registerPermissionCalculator(
        type: String, calculator: PermissionCalculator<*>, subject: Subject
    ) = permissionCalculatorRegistrations.register(type, DefaultRegistration(calculator, subject))

    fun unregisterPermissionCalculatorByType(type: String) = permissionCalculatorRegistrations.unregisterByKey(type)
    fun unregisterPermissionCalculatorBySubject(subject: Subject) =
        permissionCalculatorRegistrations.unregisterBySubject(subject)

    fun registerPermissionMeta(id: SegmentId, meta: PermissionMeta, subject: Subject) =
        permissionMetaRegistrations.register(id, meta, subject)

    fun unregisterPermissionMetaById(id: SegmentId) = permissionMetaRegistrations.unregisterById(id)
    fun unregisterPermissionMetaBySubject(subject: Subject) = permissionMetaRegistrations.unregisterBySubject(subject)
    fun unregisterPermissionMetaByIdAndSubject(id: SegmentId, subject: Subject) =
        permissionMetaRegistrations.unregisterByIdAndSubject(id, subject)

    fun registerPermissionSettingChecker(type: String, checker: PermissionSettingChecker, subject: Subject) =
        permissionSettingCheckerRegistrations.register(type, DefaultRegistration(checker, subject))

    fun unregisterPermissionSettingCheckerByType(type: String) =
        permissionSettingCheckerRegistrations.unregisterByKey(type)

    fun unregisterPermissionSettingCheckerBySubject(subject: Subject) =
        permissionSettingCheckerRegistrations.unregisterBySubject(subject)

    fun unregisterBySubject(subject: Subject) {
        unregisterPermissionCalculatorBySubject(subject)
        unregisterPermissionMetaBySubject(subject)
        unregisterPermissionSettingCheckerBySubject(subject)
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