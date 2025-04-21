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

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.Subject
import cn.codethink.xiaoming.util.Operation

class PermissionTestContextImpl<T : Subject>(
    override val platform: LocalPlatform,
    override val manager: LocalPermissionManager,
    override val permission: Permission,
    override val subject: T,
    val operation: Operation,
    override val handler: Registration<PermissionHandler<T>>
) : PermissionTestContext<T>