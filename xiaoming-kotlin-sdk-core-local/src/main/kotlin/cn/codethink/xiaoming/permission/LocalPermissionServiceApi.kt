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

import cn.codethink.xiaoming.common.DefaultListRegistrations
import cn.codethink.xiaoming.common.DefaultMapRegistrations
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Registrations
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.permission.data.LocalPlatformConfiguration
import cn.codethink.xiaoming.permission.data.PermissionProfile
import io.github.oshai.kotlinlogging.KLogger
import java.util.concurrent.CopyOnWriteArrayList

class LocalPermissionServiceApi(
    val logger: KLogger,
    val configuration: LocalPlatformConfiguration
) {
    val data by configuration::data
    val permissionProfileServices = DefaultStringMapRegistrations<PermissionProfileService>()

    val permissionComparators = PermissionComparatorRegistrations()
    val defaultPermissionComparators = DefaultListRegistrations<PermissionComparator>()

    val permissionMetas = DefaultMapRegistrations<SegmentId, PermissionMeta>()

    fun hasPermission(profile: PermissionProfile, permission: Permission): Boolean? {
        data.permissionRecords.getRecords(profile).forEach {
            val context = PermissionComparingContext(profile, permission, it, this)
            var comparatorRegistrations: List<Registration<PermissionComparator>> =
                permissionComparators.getComparators(permission.descriptor.node)

            if (comparatorRegistrations.isEmpty()) {
                comparatorRegistrations = defaultPermissionComparators.toList()
            }

            comparatorRegistrations.forEach { registration ->
                if (registration.value.compare(context)) {
                    return it.value
                }
            }
        }
        return null
    }
}


class PermissionComparatorRegistrations : Registrations<PermissionComparator> {
    data class PermissionComparatorRegistration(
        val matcher: Matcher<SegmentId>,
        override val value: PermissionComparator,
        override val subject: Subject
    ) : Registration<PermissionComparator>

    val comparators = CopyOnWriteArrayList<PermissionComparatorRegistration>()

    fun getComparators(id: SegmentId): List<PermissionComparatorRegistration> =
        comparators.filter { it.matcher.isMatched(id) }

    override fun unregisterBySubject(subject: Subject): Boolean = comparators.removeAll { it.subject == subject }
}
