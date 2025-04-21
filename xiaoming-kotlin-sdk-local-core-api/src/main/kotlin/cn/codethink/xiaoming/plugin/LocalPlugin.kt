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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.Platform
import cn.codethink.xiaoming.util.Operation

/**
 * 表示代码在当前进程里运行的插件。
 *
 * @author Chuanwise
 */
interface LocalPlugin : Plugin {
    /**
     * 插件的运行时元数据。
     * 本地插件可以为多个宿主服务，每个宿主都有一份元数据。
     */
    val entries: Map<Platform, PluginEntry>
}