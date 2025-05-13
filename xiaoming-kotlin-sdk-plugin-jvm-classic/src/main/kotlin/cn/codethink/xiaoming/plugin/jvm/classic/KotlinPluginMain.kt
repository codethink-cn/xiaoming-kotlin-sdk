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

@PluginMain(KotlinPluginHandlerFactory::class)
interface KotlinPluginMain {
    /**
     * 执行插件加载操作。
     *
     * @param context 插件加载上下文
     */
    suspend fun onLoad(context: KotlinPluginMainLoadContext) = Unit

    /**
     * 执行插件启动操作。
     *
     * @param context 插件启动上下文
     */
    suspend fun onEnable(context: KotlinPluginMainEnableContext) = Unit

    /**
     * 执行插件关闭操作。
     *
     * @param context 插件关闭上下文
     */
    suspend fun onDisable(context: KotlinPluginMainDisableContext) = Unit

    /**
     * 执行插件卸载操作。
     *
     * @param context 插件卸载上下文
     */
    suspend fun onUnload(context: KotlinPluginMainUnloadContext) = Unit
}