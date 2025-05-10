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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.RemotePlatform
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.Operation

/**
 * 可以在多个宿主间共享的插件。
 *
 * @author Chuanwise
 */
interface SharablePlugin : Plugin {
    /**
     * 插件可以为多个宿主服务，此为其对应于不同宿主的插件对象。其中不包含当前宿主本身。
     */
    val instances: Map<RemotePlatform, RemoteServingPlugin>

    /**
     * 注册一个宿主服务实例。
     *
     * @param platform 宿主平台。
     * @param handler 插件处理器。
     * @param operation 操作。
     * @return 注册的宿主服务实例。
     */
    @ExperimentalApi
    fun registerInstance(platform: RemotePlatform, handler: PluginHandler, operation: Operation): RemoteServingPlugin
}