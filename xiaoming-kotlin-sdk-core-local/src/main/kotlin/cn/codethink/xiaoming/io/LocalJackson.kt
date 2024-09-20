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

import cn.codethink.xiaoming.data.LocalPlatformData
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import cn.codethink.xiaoming.io.data.polymorphic
import cn.codethink.xiaoming.io.data.subType
import cn.codethink.xiaoming.permission.DEFAULT_PERMISSION_COMPARATOR_FIELD_VERSION
import cn.codethink.xiaoming.permission.DEFAULT_PERMISSION_COMPARATOR_VERSION_1
import cn.codethink.xiaoming.permission.DefaultPermissionComparator
import cn.codethink.xiaoming.permission.DefaultPermissionComparatorV1
import cn.codethink.xiaoming.permission.INHERITANCE_PERMISSION_COMPARATOR_FIELD_VERSION
import cn.codethink.xiaoming.permission.INHERITANCE_PERMISSION_COMPARATOR_VERSION_1
import cn.codethink.xiaoming.permission.InheritancePermissionComparator
import cn.codethink.xiaoming.permission.InheritancePermissionComparatorV1
import cn.codethink.xiaoming.permission.PERMISSION_COMPARATOR_TYPE_DEFAULT
import cn.codethink.xiaoming.permission.PERMISSION_COMPARATOR_TYPE_INHERITANCE
import cn.codethink.xiaoming.permission.PermissionComparator
import com.fasterxml.jackson.databind.module.SimpleModule

class LocalPlatformModule : SimpleModule(
    "LocalPlatformModule", XiaomingJacksonModuleVersion
) {
    inner class Deserializers {
        val platformData = polymorphic<LocalPlatformData>()
        val permissionComparator = polymorphic<PermissionComparator> {
            subType<DefaultPermissionComparator>(PERMISSION_COMPARATOR_TYPE_DEFAULT)
            subType<InheritancePermissionComparator>(PERMISSION_COMPARATOR_TYPE_INHERITANCE)
        }
        val inheritancePermissionComparator =
            polymorphic<InheritancePermissionComparator>(INHERITANCE_PERMISSION_COMPARATOR_FIELD_VERSION) {
                subType<InheritancePermissionComparatorV1>(INHERITANCE_PERMISSION_COMPARATOR_VERSION_1)
            }
        val defaultPermissionComparator =
            polymorphic<DefaultPermissionComparator>(DEFAULT_PERMISSION_COMPARATOR_FIELD_VERSION) {
                subType<DefaultPermissionComparatorV1>(DEFAULT_PERMISSION_COMPARATOR_VERSION_1)
            }
    }

    val deserializers: Deserializers = Deserializers()
}