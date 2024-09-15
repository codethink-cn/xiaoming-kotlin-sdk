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

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_DATA
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

class LocalPlatformConfiguration(
    raw: Raw
) : AbstractData(raw) {

    val data: LocalPlatformData by raw

    @JvmOverloads
    constructor(
        data: LocalPlatformData,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[LOCAL_PERMISSION_SERVICE_CONFIGURATION_FIELD_DATA] = data
    }
}