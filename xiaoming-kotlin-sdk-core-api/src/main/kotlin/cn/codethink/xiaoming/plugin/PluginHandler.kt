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
 * 插件处理器：实现插件的分配、加载、启动、关闭、卸载和释放操作。
 *
 * 其回调函数可随时使用 [PluginContext.crash] 标记插件已崩溃。
 *
 * @author Chuanwise
 */
interface PluginHandler {
    /**
     * 执行插件分配操作。
     *
     * 于本地插件而言，其分配意味着开始读取和解析二进制文件或脚本。而于远程插件而言，
     * 其意味着网络资源或其他进程间通讯资源的申请。
     *
     * @param context 插件分配上下文
     */
    @Throws(Exception::class)
    suspend fun onAllocate(context: PluginAllocateContext)

    /**
     * 执行插件加载操作。
     *
     * 于本地插件而言，其加载意味着开始执行一段代码以加载插件的资源。而于远程插件而言，
     * 其意味着网络连接的建立和握手。
     *
     * @param context 插件加载上下文
     */
    @Throws(Exception::class)
    suspend fun onLoad(context: PluginLoadContext)

    /**
     * 执行插件启动操作。
     *
     * 插件可以向宿主注册指令、监听器等内容，以便嵌入功能。
     *
     * @param context 插件启动上下文
     */
    @Throws(Exception::class)
    suspend fun onEnable(context: PluginEnableContext)

    /**
     * 执行插件关闭操作。
     *
     * 不论该函数是否正常退出，所有其向宿主注册的指令、监听器等内容都将被注销。
     *
     * @param context 插件关闭上下文
     */
    @Throws(Exception::class)
    suspend fun onDisable(context: PluginDisableContext)

    /**
     * 执行插件卸载操作。
     *
     * 于本地插件而言，其卸载意味着开始执行一段代码以卸载插件的资源。而于远程插件而言，
     * 其意味着网络连接的断开。
     *
     * @param context 插件卸载上下文
     */
    @Throws(Exception::class)
    suspend fun onUnload(context: PluginUnloadContext)

    /**
     * 执行插件释放操作。
     *
     * 于本地插件而言，其释放意味着开始释放二进制文件或脚本的资源。而于远程插件而言，
     * 其意味着网络资源或其他进程间通讯资源的释放。
     *
     * 插件可能在 [PluginState.UNALLOCATED] 或 [PluginState.CRASHED] 状态时被释放。
     *
     * @param context 插件释放上下文
     */
    @Throws(Exception::class)
    suspend fun onExit(context: PluginExitContext)
}