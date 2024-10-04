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
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Registrations
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.SubjectDescriptor
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
        caller: SubjectDescriptor? = null, cause: Cause? = null
    ) {
        // Check if operation valid.
        permissionSettingCheckerRegistrations[comparator.type]?.let {
            val addingContext = PermissionSettingContext(this, profile, comparator, contextMatchers, caller, cause)
            it.value.check(addingContext)
        }

        // Do operation.
        val records = internalApi.data.getPermissionRecordsByPermissionProfileId(profile.id)

        val previousRecords = records.filter { it.comparator == comparator && it.contextMatchers == contextMatchers }
        if (previousRecords.isNotEmpty()) {
            if (previousRecords.size == 1) {
                val thatOne = previousRecords.first()
                logger.warn { "Permission record already set: $thatOne." }
                return
            }
            internalApi.data.deletePermissionRecords(previousRecords)
        }

        // Add new record.
        internalApi.data.insertPermissionRecord(profile, comparator, contextMatchers)
    }

    fun unsetPermission(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        contextMatchers: Map<String, Matcher<Any?>> = emptyMap(),
        caller: SubjectDescriptor? = null, cause: Cause? = null
    ): Boolean {
        val records = internalApi.data.getPermissionRecordsByPermissionProfileId(profile.id)
            .filter { it.comparator == comparator && it.contextMatchers == contextMatchers }

        if (records.isNotEmpty()) {
            internalApi.data.deletePermissionRecords(records)
        }

        return records.isNotEmpty()
    }

    fun hasPermission(
        profileId: Id, permission: Permission,
        context: Map<String, Any?> = emptyMap(),
        caller: SubjectDescriptor? = null, cause: Cause? = null
    ): Boolean? {
        internalApi.data.getPermissionRecordsByPermissionProfileId(profileId).forEach {
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
        caller: SubjectDescriptor? = null, cause: Cause? = null
    ): Boolean? {
        internalApi.data.getPermissionRecordsByPermissionProfileId(profile.id).forEach {
            val comparingContext = PermissionComparingContext(
                this, profile.id, permission, it, context, caller, cause
            )
            it.comparator.compare(comparingContext)?.let { result -> return result.value }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun hasPermission(
        subjectDescriptor: SubjectDescriptor, permission: Permission,
        context: Map<String, Any?> = emptyMap(), caller: SubjectDescriptor? = null, cause: Cause? = null
    ): Boolean? {
        val calculator =
            permissionCalculatorRegistrations[subjectDescriptor.type]?.value as PermissionCalculator<SubjectDescriptor>? ?: return null
        val calculatingContext = PermissionCalculatingContext(this, subjectDescriptor, permission, context, caller, cause)

        return calculator.hasPermission(calculatingContext)
    }

    fun registerPermissionCalculator(
        type: String, calculator: PermissionCalculator<*>, subjectDescriptor: SubjectDescriptor
    ) = permissionCalculatorRegistrations.register(type, DefaultRegistration(calculator, subjectDescriptor))

    fun unregisterPermissionCalculatorByType(type: String) = permissionCalculatorRegistrations.unregisterByKey(type)
    fun unregisterPermissionCalculatorBySubject(subjectDescriptor: SubjectDescriptor) =
        permissionCalculatorRegistrations.unregisterBySubject(subjectDescriptor)

    fun registerPermissionMeta(id: SegmentId, meta: PermissionMeta, subjectDescriptor: SubjectDescriptor) =
        permissionMetaRegistrations.register(id, meta, subjectDescriptor)

    fun unregisterPermissionMetaById(id: SegmentId) = permissionMetaRegistrations.unregisterById(id)
    fun unregisterPermissionMetaBySubject(subjectDescriptor: SubjectDescriptor) = permissionMetaRegistrations.unregisterBySubject(subjectDescriptor)
    fun unregisterPermissionMetaByIdAndSubject(id: SegmentId, subjectDescriptor: SubjectDescriptor) =
        permissionMetaRegistrations.unregisterByIdAndSubject(id, subjectDescriptor)

    fun registerPermissionSettingChecker(type: String, checker: PermissionSettingChecker, subjectDescriptor: SubjectDescriptor) =
        permissionSettingCheckerRegistrations.register(type, DefaultRegistration(checker, subjectDescriptor))

    fun unregisterPermissionSettingCheckerByType(type: String) =
        permissionSettingCheckerRegistrations.unregisterByKey(type)

    fun unregisterPermissionSettingCheckerBySubject(subjectDescriptor: SubjectDescriptor) =
        permissionSettingCheckerRegistrations.unregisterBySubject(subjectDescriptor)

    fun unregisterBySubject(subjectDescriptor: SubjectDescriptor) {
        unregisterPermissionCalculatorBySubject(subjectDescriptor)
        unregisterPermissionMetaBySubject(subjectDescriptor)
        unregisterPermissionSettingCheckerBySubject(subjectDescriptor)
    }
}

class PermissionComparatorRegistrations : Registrations {
    data class PermissionComparatorRegistration(
        val matcher: Matcher<SegmentId>,
        override val value: PermissionComparator,
        override val subjectDescriptor: SubjectDescriptor
    ) : Registration<PermissionComparator>

    private val comparators = CopyOnWriteArrayList<PermissionComparatorRegistration>()
    operator fun get(id: SegmentId) = comparators.filter { it.matcher.isMatched(id) }

    fun register(matcher: Matcher<SegmentId>, comparator: PermissionComparator, subjectDescriptor: SubjectDescriptor) {
        comparators.add(PermissionComparatorRegistration(matcher, comparator, subjectDescriptor))
    }

    override fun unregisterBySubject(subjectDescriptor: SubjectDescriptor): Boolean = comparators.removeAll { it.subjectDescriptor == subjectDescriptor }
    fun unregisterIfMatched(id: SegmentId): Boolean = comparators.removeAll { it.matcher.isMatched(id) }
}

class PermissionMetaRegistrations : Registrations {
    class PermissionMetaRegistration(
        val id: SegmentId,
        override val value: PermissionMeta,
        override val subjectDescriptor: SubjectDescriptor
    ) : Registration<PermissionMeta>

    private val lock = ReentrantReadWriteLock()

    private val metaBySegmentId = mutableMapOf<SegmentId, MutableMap<SubjectDescriptor, PermissionMetaRegistration>>()
    private val metaBySubjectDescriptor = mutableMapOf<SubjectDescriptor, MutableMap<SegmentId, PermissionMetaRegistration>>()

    operator fun get(id: SegmentId): Map<SubjectDescriptor, PermissionMetaRegistration>? = lock.read {
        metaBySegmentId[id]
    }

    operator fun get(subjectDescriptor: SubjectDescriptor): Map<SegmentId, PermissionMetaRegistration>? = lock.read {
        metaBySubjectDescriptor[subjectDescriptor]
    }

    operator fun get(id: SegmentId, subjectDescriptor: SubjectDescriptor): PermissionMetaRegistration? = lock.read {
        metaBySegmentId[id]?.get(subjectDescriptor)
    }

    fun register(id: SegmentId, meta: PermissionMeta, subjectDescriptor: SubjectDescriptor) {
        val registration = PermissionMetaRegistration(id, meta, subjectDescriptor)

        lock.write {
            metaBySegmentId.getOrPut(id) { mutableMapOf() }[subjectDescriptor] = registration
            metaBySubjectDescriptor.getOrPut(subjectDescriptor) { mutableMapOf() }[id] = registration
        }
    }

    override fun unregisterBySubject(subjectDescriptor: SubjectDescriptor): Boolean = lock.write {
        val registrations = metaBySubjectDescriptor.remove(subjectDescriptor) ?: return false
        registrations.values.forEach { registration ->
            metaBySegmentId[registration.id]?.remove(subjectDescriptor)
        }
        return true
    }

    fun unregisterById(id: SegmentId): Boolean = lock.write {
        val registrations = metaBySegmentId.remove(id) ?: return false
        registrations.values.forEach { registration ->
            metaBySubjectDescriptor[registration.subjectDescriptor]?.remove(id)
        }
        return true
    }

    fun unregisterByIdAndSubject(id: SegmentId, subjectDescriptor: SubjectDescriptor): Boolean = lock.write {
        val registration = metaBySegmentId[id]?.remove(subjectDescriptor) ?: return false
        metaBySubjectDescriptor[subjectDescriptor]?.remove(id)
        return true
    }
}