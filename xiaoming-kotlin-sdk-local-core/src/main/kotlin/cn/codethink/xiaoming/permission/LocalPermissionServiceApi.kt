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

import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.RegistrationImpl
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.Matcher
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.RegistrationManager
import cn.codethink.xiaoming.util.SegmentIdImpl
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.util.MutableStringMapRegistrationManagerImpl
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LocalPermissionServiceApi(
    val internalApi: LocalPlatformInternalApi
) {
    val logger by internalApi::logger

    val permissionCalculatorRegistrations =
        MutableStringMapRegistrationManagerImpl<PermissionCalculator<SubjectDescriptor>>()
    val permissionContextMatcherRegistrations = MutableStringMapRegistrationManagerImpl<Matcher<*>>()

    val permissionMetaRegistrations = PermissionMetaRegistrationManager()

    val permissionSettingCheckerRegistrations = MutableStringMapRegistrationManagerImpl<PermissionSettingChecker>()

    init {
        registerPermissionSettingChecker(
            PERMISSION_COMPARATOR_TYPE_INHERITANCE, InheritancePermissionSettingChecker, internalApi.descriptor
        )
    }

    fun setPermission(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        cause: Cause,
        context: Map<String, Matcher<Any?>> = emptyMap()
    ) {
        logger.trace { "Setting permission for profile $profile: $comparator, context: $context caused by $cause." }

        // Check if operation valid.
        permissionSettingCheckerRegistrations[comparator.type]?.let {
            val addingContext =
                PermissionSettingContext(this, profile, comparator, context, cause)
            it.value.check(addingContext)
        }

        // Do operation.
        val records = internalApi.data.getPermissionRecordsByPermissionProfileId(profile.id)

        val previousRecords = records.filter { it.comparator == comparator && it.context == context }
        if (previousRecords.isNotEmpty()) {
            if (previousRecords.size == 1) {
                val thatOne = previousRecords.first()
                logger.warn { "Permission record already set: $thatOne." }
                return
            }
            internalApi.data.deletePermissionRecords(previousRecords)
        }

        // Add new record.
        internalApi.data.insertPermissionRecord(profile, comparator, context)
    }

    fun unsetPermission(
        profile: PermissionProfile,
        comparator: PermissionComparator,
        context: Map<String, Matcher<Any?>> = emptyMap(),
        cause: Cause? = null, subject: SubjectDescriptor? = null
    ): Boolean {
        val records = internalApi.data.getPermissionRecordsByPermissionProfileId(profile.id)
            .filter { it.comparator == comparator && it.context == context }

        if (records.isNotEmpty()) {
            internalApi.data.deletePermissionRecords(records)
        }

        return records.isNotEmpty()
    }

    fun hasPermission(
        profile: Id,
        permission: Permission,
        cause: Cause,
        context: Map<String, Any?> = emptyMap()
    ): Boolean? {
        logger.trace { "Checking for profile $profile permission: $permission, context: $context caused by $cause." }

        val records = internalApi.data.getPermissionRecordsByPermissionProfileId(profile)
        for (record in records) {
            // 1. Check context. Extra context matcher provided is allowed.
            val allContextMatched = record.context.all { (contextKey, contextMatcher) ->
                val contextValue = context[contextKey]
                return (contextMatcher.isMatched(contextValue)).apply {
                    logger.trace { "Checking record: $record, context $contextKey: $contextValue matched $contextMatcher: $this." }
                }
            }
            if (!allContextMatched) {
                continue
            }

            // 2. Use permission comparator.
            val comparingContext = PermissionComparingContext(
                this, profile, permission, record, context, cause
            )
            record.comparator.compare(comparingContext)?.let { result ->
                logger.trace { "Permission record $record compare result: $result." }
                return result.value
            }
        }

        logger.trace { "No permission record found for profile $profile." }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun hasPermission(
        target: SubjectDescriptor,
        permission: Permission,
        cause: Cause,
        context: Map<String, Any?> = emptyMap()
    ): Boolean? {
        logger.trace { "Checking for target $target permission: $permission, context: $context caused by $cause." }

        val calculator =
            permissionCalculatorRegistrations[target.type]?.value as PermissionCalculator<SubjectDescriptor>?
        if (calculator == null) {
            logger.warn { "No permission calculator found for target $target." }
            return null
        }

        val calculatingContext = PermissionCalculatingContext(
            this, target, permission, context, cause
        )

        return calculator.hasPermission(calculatingContext)
    }

    fun registerPermissionCalculator(
        type: String, calculator: PermissionCalculator<*>, subject: SubjectDescriptor
    ) {
        permissionCalculatorRegistrations[type] = RegistrationImpl(calculator, subject)
    }

    fun unregisterPermissionCalculatorByType(type: String) = permissionCalculatorRegistrations.remove(type)
    fun unregisterPermissionCalculatorBySubject(subject: SubjectDescriptor) =
        permissionCalculatorRegistrations.unregisterBySubject(subject)

    fun registerPermissionMeta(id: SegmentIdImpl, meta: PermissionMeta, subject: SubjectDescriptor) =
        permissionMetaRegistrations.register(id, meta, subject)

    fun unregisterPermissionMetaById(id: SegmentIdImpl) = permissionMetaRegistrations.unregisterById(id)
    fun unregisterPermissionMetaBySubject(subject: SubjectDescriptor) =
        permissionMetaRegistrations.unregisterBySubject(subject)

    fun unregisterPermissionMetaByIdAndSubject(id: SegmentIdImpl, subject: SubjectDescriptor) =
        permissionMetaRegistrations.unregisterByIdAndSubject(id, subject)

    fun registerPermissionSettingChecker(
        type: String,
        checker: PermissionSettingChecker,
        subject: SubjectDescriptor
    ) = permissionSettingCheckerRegistrations.register(type, RegistrationImpl(checker, subject))

    fun unregisterPermissionSettingCheckerByType(type: String) =
        permissionSettingCheckerRegistrations.unregisterByKey(type)

    fun unregisterPermissionSettingCheckerBySubject(subject: SubjectDescriptor) =
        permissionSettingCheckerRegistrations.unregisterBySubject(subject)

    fun unregisterBySubject(subject: SubjectDescriptor) {
        unregisterPermissionCalculatorBySubject(subject)
        unregisterPermissionMetaBySubject(subject)
        unregisterPermissionSettingCheckerBySubject(subject)
    }
}

fun LocalPermissionServiceApi.hasPermission(
    profile: PermissionProfile, permission: Permission,
    cause: Cause,
    context: Map<String, Any?> = emptyMap()
): Boolean? = hasPermission(profile.id, permission, cause, context)

class PermissionComparatorRegistrationManager : RegistrationManager {
    data class PermissionComparatorRegistration(
        val matcher: Matcher<SegmentIdImpl>,
        override val value: PermissionComparator,
        override val subject: SubjectDescriptor
    ) : Registration<PermissionComparator>

    private val comparators = CopyOnWriteArrayList<PermissionComparatorRegistration>()
    operator fun get(id: SegmentIdImpl) = comparators.filter { it.matcher.isMatched(id) }

    fun register(matcher: Matcher<SegmentIdImpl>, comparator: PermissionComparator, subject: SubjectDescriptor) {
        comparators.add(PermissionComparatorRegistration(matcher, comparator, subject))
    }

    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean =
        comparators.removeAll { it.subject == subject }

    fun unregisterIfMatched(id: SegmentIdImpl): Boolean = comparators.removeAll { it.matcher.isMatched(id) }
}

class PermissionMetaRegistrationManager : RegistrationManager {
    class PermissionMetaRegistration(
        val id: SegmentIdImpl,
        override val value: PermissionMeta,
        override val subject: SubjectDescriptor
    ) : Registration<PermissionMeta>

    private val lock = ReentrantReadWriteLock()

    private val metaBySegmentId =
        mutableMapOf<SegmentIdImpl, MutableMap<SubjectDescriptor, PermissionMetaRegistration>>()
    private val metaBySubjectDescriptor =
        mutableMapOf<SubjectDescriptor, MutableMap<SegmentIdImpl, PermissionMetaRegistration>>()

    operator fun get(id: SegmentIdImpl): Map<SubjectDescriptor, PermissionMetaRegistration>? = lock.read {
        metaBySegmentId[id]
    }

    operator fun get(subject: SubjectDescriptor): Map<SegmentIdImpl, PermissionMetaRegistration>? = lock.read {
        metaBySubjectDescriptor[subject]
    }

    operator fun get(id: SegmentIdImpl, subject: SubjectDescriptor): PermissionMetaRegistration? = lock.read {
        metaBySegmentId[id]?.get(subject)
    }

    fun register(id: SegmentIdImpl, meta: PermissionMeta, subject: SubjectDescriptor) {
        val registration = PermissionMetaRegistration(id, meta, subject)

        lock.write {
            metaBySegmentId.getOrPut(id) { mutableMapOf() }[subject] = registration
            metaBySubjectDescriptor.getOrPut(subject) { mutableMapOf() }[id] = registration
        }
    }

    override fun unregisterBySubject(subject: SubjectDescriptor): Boolean = lock.write {
        val registrations = metaBySubjectDescriptor.remove(subject) ?: return false
        registrations.values.forEach { registration ->
            metaBySegmentId[registration.id]?.remove(subject)
        }
        return true
    }

    fun unregisterById(id: SegmentIdImpl): Boolean = lock.write {
        val registrations = metaBySegmentId.remove(id) ?: return false
        registrations.values.forEach { registration ->
            metaBySubjectDescriptor[registration.subject]?.remove(id)
        }
        return true
    }

    fun unregisterByIdAndSubject(id: SegmentIdImpl, subject: SubjectDescriptor): Boolean = lock.write {
        val registration = metaBySegmentId[id]?.remove(subject) ?: return false
        metaBySubjectDescriptor[subject]?.remove(id)
        return true
    }
}