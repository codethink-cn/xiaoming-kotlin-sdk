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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.event.EventManager
import cn.codethink.xiaoming.exception.ExceptionManager
import cn.codethink.xiaoming.permission.PermissionManager
import cn.codethink.xiaoming.plugin.PluginManager
import cn.codethink.xiaoming.serialization.CodecResolver
import cn.codethink.xiaoming.util.Subject
import kotlinx.coroutines.CoroutineScope

/**
 * 平台：插件化框架宿主程序。
 *
 * @author Chuanwise
 */
interface Platform : Subject, CoroutineScope {
    val permissionManager: PermissionManager

    val exceptionManager: ExceptionManager

    val serializationManager: CodecResolver

    val eventManager: EventManager

    val pluginManager: PluginManager
}
