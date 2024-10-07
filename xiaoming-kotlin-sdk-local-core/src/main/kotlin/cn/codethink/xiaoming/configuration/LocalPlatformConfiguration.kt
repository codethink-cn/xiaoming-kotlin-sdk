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


@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.configuration

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.getValue
import cn.codethink.xiaoming.common.setValue
import cn.codethink.xiaoming.data.LocalPlatformDataConfiguration
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw

/**
 * Configuration storing the data of local platform.
 *
 * @author Chuanwise
 */
class LocalPlatformConfiguration : AbstractData {
    var data: LocalPlatformDataConfiguration by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        data: LocalPlatformDataConfiguration,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.data = data
    }
}