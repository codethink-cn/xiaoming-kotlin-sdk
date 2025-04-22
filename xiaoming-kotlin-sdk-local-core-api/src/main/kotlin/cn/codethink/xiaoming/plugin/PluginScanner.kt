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

/**
 * 检测当前宿主上所有已经安装的插件，或者打算安装的插件。
 *
 * @author Chuanwise
 */
fun interface PluginScanner {
    /**
     * 执行一次插件检测。
     *
     * 实现类通过 [PluginScanContext.registerPlugin] 添加一个检测到的结果，添加前应当做安全检查。
     *
     * @param context 插件检测上下文。
     */
    fun scan(context: PluginScanContext)
}