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

package cn.codethink.xiaoming.io

import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.FIELD_VERSION
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.connection.ConnectionManagerConfiguration
import cn.codethink.xiaoming.connection.ConnectionManagerConfigurationV1
import cn.codethink.xiaoming.io.data.PolymorphicDeserializerInitializer
import cn.codethink.xiaoming.io.data.PolymorphicDeserializers
import cn.codethink.xiaoming.io.data.name
import cn.codethink.xiaoming.io.data.names
import cn.codethink.xiaoming.io.data.subject
import cn.codethink.xiaoming.permission.WildCardPermissionMatcher
import cn.codethink.xiaoming.permission.SimplePermissionFilterV1
import cn.codethink.xiaoming.permission.InheritancePermissionMatcher
import cn.codethink.xiaoming.permission.InheritancePermissionMatcherV1

class LocalPlatformPolymorphicDeserializerInitializer : PolymorphicDeserializerInitializer {
    override fun initialize(deserializers: PolymorphicDeserializers, subject: SubjectDescriptor) {
        deserializers.subject(subject) {
            names<PermissionMatcher>(FIELD_TYPE) {
                names<WildCardPermissionMatcher>(FIELD_VERSION) {
                    name<SimplePermissionFilterV1>()
                }
                names<InheritancePermissionMatcher>(FIELD_VERSION) {
                    name<InheritancePermissionMatcherV1>()
                }
            }
            names<ConnectionManagerConfiguration>(FIELD_VERSION) {
                name<ConnectionManagerConfigurationV1>()
            }
        }
    }
}