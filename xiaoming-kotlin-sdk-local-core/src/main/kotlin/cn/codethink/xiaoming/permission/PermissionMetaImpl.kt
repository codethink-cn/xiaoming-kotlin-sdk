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

import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRaw
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.getValue
import cn.codethink.xiaoming.util.setValue

class PermissionMetaImpl : AbstractData, PermissionMeta {
    override var id: NamespaceId by raw
    override var subject: SubjectDescriptor by raw
    override var parameters: Map<String, PermissionParameterMeta> by raw
    override var description: String by raw
    override val descriptor: PermissionDescriptor = PermissionDescriptorImpl(id, subject)

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: NamespaceId,
        subject: SubjectDescriptor,
        parameters: Map<String, PermissionParameterMeta> = emptyMap(),
        description: String,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.id = id
        this.subject = subject
        this.parameters = parameters
        this.description = description
    }
}

class PermissionParameterMetaImpl : AbstractData, PermissionParameterMeta {
    override var name: String by raw
    override var description: String by raw
    override var optional: Boolean by raw
    override var nullable: Boolean by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        name: String,
        description: String?,
        optional: Boolean,
        nullable: Boolean,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.name = name
        this.description = description ?: name
        this.optional = optional
        this.nullable = nullable
    }
}

