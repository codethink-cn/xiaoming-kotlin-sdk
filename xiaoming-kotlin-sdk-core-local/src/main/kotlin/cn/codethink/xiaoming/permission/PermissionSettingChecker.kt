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
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.permission.data.PermissionProfile


data class PermissionSettingContext(
    val api: LocalPermissionServiceApi,
    val profile: PermissionProfile,
    val subjectMatcher: Matcher<Subject>,
    val nodeMatcher: Matcher<SegmentId>,
    val value: Boolean?,
    val argumentMatchers: Map<String, Matcher<*>> = emptyMap(),
    val context: Map<String, Any?> = emptyMap(),
    val caller: Subject? = null,
    val cause: Cause? = null
)

/**
 * Call when setting a permission.
 *
 * @author Chuanwise
 * @see LocalPermissionServiceApi.setPermission
 */
interface PermissionSettingChecker {
    fun check(context: PermissionSettingContext)
}
