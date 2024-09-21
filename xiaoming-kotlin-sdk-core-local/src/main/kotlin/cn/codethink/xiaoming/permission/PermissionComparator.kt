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
import cn.codethink.xiaoming.common.Data
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.data.getPermissionProfileOrFail
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord

class PermissionComparingContext(
    val permissionServiceApi: LocalPermissionServiceApi,
    val profileId: Id,
    val permission: Permission,
    val record: PermissionRecord,
    val context: Map<String, Any?> = emptyMap(),
    val caller: Subject? = null,
    val cause: Cause? = null
) {
    val internalApi by permissionServiceApi::internalApi
    val profile: PermissionProfile by lazy {
        internalApi.data.getPermissionProfileOrFail(profileId)
    }
}

/**
 * Compare given permission node and required permission.
 *
 * @author Chuanwise
 * @see DefaultPermissionComparator
 */
interface PermissionComparator : Data {
    val type: String
    fun compare(context: PermissionComparingContext): Tristate?
}
