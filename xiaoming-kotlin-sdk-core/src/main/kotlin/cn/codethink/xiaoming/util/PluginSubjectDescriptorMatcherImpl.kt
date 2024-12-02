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

private const val SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN = "subject.plugin"

@JsonTypeName(SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN)
class PluginSubjectDescriptorMatcherImpl : AbstractData, PluginSubjectDescriptorMatcher {
    private var type: String by raw
    override var id: Matcher<NamespaceId> by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Matcher<NamespaceId>,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.type = SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN
        this.id = id
    }

    override fun isMatched(target: PluginSubjectDescriptor): Boolean {
        return id.isMatched(target.id)
    }
}