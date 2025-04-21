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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.NamespaceIdMatcher
import cn.codethink.xiaoming.util.NotStableForInheritance
import cn.codethink.xiaoming.util.SegmentIdMatcher
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * 通配权限匹配：请求的权限节点与该过滤器的节点匹配时，返回该过滤器的值。
 *
 * @author Chuanwise
 */
@NotStableForInheritance
@JsonTypeName(WildCardPermissionMatcher.TYPE)
interface WildCardPermissionMatcher : PermissionMatcher {
    companion object {
        const val TYPE = "wild_card"
    }

    override val type: String get() = TYPE

    val id: NamespaceIdMatcher
    val value: Boolean?
}