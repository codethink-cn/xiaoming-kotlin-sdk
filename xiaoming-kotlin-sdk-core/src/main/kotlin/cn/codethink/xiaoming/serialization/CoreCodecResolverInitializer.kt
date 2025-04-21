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

package cn.codethink.xiaoming.serialization

import cn.codethink.xiaoming.serialization.dsl.fallbackHint
import cn.codethink.xiaoming.serialization.dsl.hint
import cn.codethink.xiaoming.serialization.dsl.registering
import cn.codethink.xiaoming.serialization.dsl.string
import cn.codethink.xiaoming.serialization.dsl.type
import cn.codethink.xiaoming.serialization.dsl.typeField
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.CauseImpl
import cn.codethink.xiaoming.util.PluginSubjectDescriptor
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionImpl
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.toVersion
import cn.codethink.xiaoming.util.toVersionMatcher

class CoreCodecResolverInitializer : CodecResolverInitializer {
    override fun initialize(context: CodecResolverInitializeContext) {
        context.registering {
            type<VersionMatcher> {
                string(
                    serializer = { it.toString() },
                    deserializer = { it.toVersionMatcher() }
                )
            }

            typeField<SubjectDescriptor> {
                hint<PluginSubjectDescriptor>("plugin")
            }

            type<Version> {
                string(
                    serializer = { it.toString() },
                    deserializer = { it.toVersion() }
                )
            }

            type<Cause> {
                fallbackHint<CauseImpl>()
            }

//            typeField<StringMatcher> {
//                hint<WildCardStringMatcher>()
//                hint<RegexStringMatcherImpl>()
//                hint<LiteralStringMatcherImpl>()
//            }

//            tokens<SegmentIdMatcher> {
//                token<SegmentIdMatcherImpl>(JsonToken.VALUE_STRING)
//            }
//
//            tokens<PluginSubjectDescriptorMatcher> {
//                token<PluginSubjectDescriptorMatcherImpl>(JsonToken.VALUE_STRING)
//            }
//
//            tokens<Id> {
//                token<LongIdImpl>(JsonToken.VALUE_NUMBER_INT)
//                token<StringId>(JsonToken.VALUE_STRING)
//            }
        }
    }
}