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

import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.FIELD_VERSION
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.connection.ConnectionManagerConfiguration
import cn.codethink.xiaoming.connection.ConnectionManagerConfigurationV1
import cn.codethink.xiaoming.io.data.PolymorphicDeserializers
import cn.codethink.xiaoming.io.data.name
import cn.codethink.xiaoming.io.data.names
import cn.codethink.xiaoming.io.data.subject
import cn.codethink.xiaoming.permission.DefaultPermissionComparator
import cn.codethink.xiaoming.permission.DefaultPermissionComparatorV1
import cn.codethink.xiaoming.permission.InheritancePermissionComparator
import cn.codethink.xiaoming.permission.InheritancePermissionComparatorV1
import cn.codethink.xiaoming.permission.PermissionComparator

fun PolymorphicDeserializers.registerLocalPlatformDeserializers(subject: Subject) = subject(subject) {
    names<PermissionComparator>(FIELD_TYPE) {
        names<DefaultPermissionComparator>(FIELD_VERSION) {
            name<DefaultPermissionComparatorV1>()
        }
        names<InheritancePermissionComparator>(FIELD_VERSION) {
            name<InheritancePermissionComparatorV1>()
        }
    }
    names<ConnectionManagerConfiguration>(FIELD_VERSION) {
        name<ConnectionManagerConfigurationV1>()
    }
}