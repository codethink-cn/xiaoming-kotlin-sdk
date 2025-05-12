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

package cn.codethink.xiaoming.plugin.jvm

/**
 * 本地 JVM 插件的类路径设置。
 *
 * @author Chuanwise
 */
interface JvmPluginClassPath {
    /**
     * 插件类访问策略。可用于实现插件间类隔离。默认为完全隔离策略。
     */
    var classAccessPolicy: JvmPluginClassAccessPolicy

    /**
     * 是否从系统类加载器中解析资源，默认为 `true`。
     */
    var resolveSystemResources: Boolean

    /**
     * 是否在类加载失败时，尝试从其他无关插件中解析类，默认为 `false`。
     */
    var resolveIndependentPluginClasses: Boolean

    /**
     * 是否允许被无关插件解析，默认为 `false`。
     */
    var allowResolvedByIndependentPlugins: Boolean

    /**
     * 插件类加载器。
     */
    val pluginClassLoader: ClassLoader
}