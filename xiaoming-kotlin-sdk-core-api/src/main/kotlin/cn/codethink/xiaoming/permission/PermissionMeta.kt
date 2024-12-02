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

import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.SubjectDescriptor

@InternalImplementedApi
interface PermissionMeta {
    val id: NamespaceId
    val subject: SubjectDescriptor
    val descriptor: PermissionDescriptor
    val parameters: Map<String, PermissionParameterMeta>
    val description: String
}


@InternalImplementedApi
interface PermissionParameterMeta {
    val description: String
    val name: String
    val optional: Boolean
    val nullable: Boolean
}