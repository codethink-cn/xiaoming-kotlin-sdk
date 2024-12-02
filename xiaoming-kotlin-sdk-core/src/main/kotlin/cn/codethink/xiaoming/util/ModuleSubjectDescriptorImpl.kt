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

package cn.codethink.xiaoming.util

import com.fasterxml.jackson.annotation.JsonTypeName

const val SUBJECT_DESCRIPTOR_TYPE_MODULE = "module"

@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_MODULE)
class ModuleSubjectDescriptorImpl : AbstractSubjectDescriptor, ModuleSubjectDescriptor {
    override var group: String by raw
    override var name: String by raw
    override var version: Version by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        group: String,
        name: String,
        version: Version,
        raw: Raw = MapRaw()
    ) : super(SUBJECT_DESCRIPTOR_TYPE_MODULE, raw) {
        this.group = group
        this.name = name
        this.version = version
    }
}