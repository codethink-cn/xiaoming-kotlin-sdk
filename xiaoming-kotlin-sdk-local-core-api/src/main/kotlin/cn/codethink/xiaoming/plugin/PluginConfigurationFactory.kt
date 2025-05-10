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

@file:JvmName("PluginConfigurationFactory")

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.api.LocalCoreApi
import cn.codethink.xiaoming.util.InternalApi

@JvmOverloads
@OptIn(InternalApi::class)
@JvmName("createPluginConfiguration")
fun PluginConfiguration(
    sharable: Boolean = false,
    debug: Boolean = false,
    crashOnRemoved: Boolean = true
): PluginConfiguration {
    return LocalCoreApi.getInstance().createPluginConfiguration(sharable, debug, crashOnRemoved)
}