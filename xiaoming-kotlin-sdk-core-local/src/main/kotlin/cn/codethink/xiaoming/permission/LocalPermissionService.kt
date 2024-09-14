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
import cn.codethink.xiaoming.common.Subject

class LocalPermissionService(
    val api: LocalPermissionServiceApi
) : PermissionService {
    override suspend fun hasPermission(
        subject: Subject,
        permission: Permission,
        caller: Subject?,
        cause: Cause?
    ): Boolean? {
        val permissionProfileServiceRegistration = api.permissionProfileServices[subject.type]
            ?: throw NoSuchElementException("No permission profile service found for subject type: ${subject.type}.")

        return permissionProfileServiceRegistration.value.hasPermission(api, subject, permission)
    }
}
