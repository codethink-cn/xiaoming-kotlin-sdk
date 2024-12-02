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

@file:JvmName("Permissions")

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.setValue
import cn.codethink.xiaoming.util.Raw

class PermissionImpl : AbstractData, Permission {
    override var descriptor: PermissionDescriptor by raw
    override var arguments: Map<String, Any?> by raw

    override val id: Id get() = descriptor.id

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        descriptor: PermissionDescriptor,
        arguments: Map<String, Any?> = emptyMap(),
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.descriptor = descriptor
        this.arguments = arguments
    }
}