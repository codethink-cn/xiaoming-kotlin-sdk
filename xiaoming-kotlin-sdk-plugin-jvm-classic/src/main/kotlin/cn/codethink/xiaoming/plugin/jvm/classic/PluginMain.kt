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

package cn.codethink.xiaoming.plugin.jvm.classic

import kotlin.reflect.KClass

/**
 * 插件主类：插件的入口。
 *
 * 插件主类通常也被称为插件。为了方便，包含一个插件主类实现的 JAR 包也可以被称为插件。
 * 插件的代码可以运行在宿主进程中，此时
 *
 * @author Chuanwise
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginMain(
    val handlerFactory: KClass<out PluginHandlerFactory>
)