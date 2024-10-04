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

package cn.codethink.xiaoming.permission.data

import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.SubjectDescriptor

/**
 * Permission profile is a set of permissions that a subject has. It can be an external user,
 * a group of users, a plugin, etc.
 *
 * @author Chuanwise
 */
interface PermissionProfile {
    val id: Id

    /**
     * The subject that this permission profile belongs to.
     */
    val subject: SubjectDescriptor
}