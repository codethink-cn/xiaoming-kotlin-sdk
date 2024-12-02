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

import com.fasterxml.jackson.core.JsonToken

/**
 * Initialize the basic polymorphic deserializers of the platform.
 *
 * @author Chuanwise
 */
class CorePolymorphicDeserializerInitializer : PolymorphicDeserializerInitializer {
    override fun initialize(deserializers: MutablePolymorphicDeserializers, subject: SubjectDescriptor) {
        deserializers.subject(subject) {
            type<VersionMatcher, AbstractVersionMatcher>()

            names<SubjectDescriptor>(FIELD_TYPE) {
                name<PluginSubjectDescriptor>()
                name<PlatformSubjectDescriptor>()
            }

            type<Version, VersionImpl>()

            names<Matcher<Any?>>(FIELD_TYPE) {
                // String matchers.
                name<WildcardStringMatcher>()
                name<RegexStringMatcherImpl>()
                name<LiteralStringMatcherImpl>()

                // SegmentId matchers.
                name<ListSegmentIdMatcherImpl>()
                name<LiteralSegmentIdMatcherImpl>()

                // Plugin subject matchers.
                name<PluginSubjectDescriptorMatcher>()

                // Permission matchers.
                name<LiteralPermissionMatcher>()
                name<DefaultPermissionMatcher>()

                // Namespace Id.
//                name<NamespaceIdMatcher>()
            }
            tokens<Id> {
                token<LongIdImpl>(JsonToken.VALUE_NUMBER_INT)
                token<StringId>(JsonToken.VALUE_STRING)
            }
        }
    }
}