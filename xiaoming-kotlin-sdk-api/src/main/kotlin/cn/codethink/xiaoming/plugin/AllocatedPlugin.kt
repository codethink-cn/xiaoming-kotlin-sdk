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
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.SegmentId

/**
 * Allocated plugin.
 *
 * @author Chuanwise
 */
interface AllocatedPlugin : Plugin {
    /**
     * Plugin's runtime meta information.
     */
    val runtime: PluginRuntime

    /**
     * Plugins provided task.
     *
     * It should NOT change plugin state.
     */
    val tasks: Map<SegmentId, PluginTask>

    /**
     * Load this plugin for specified platform.
     *
     * For local plugins, it usually means load library, initialize main class, etc.
     * Otherwise, it may mean connect to remote server and do authorization, etc.
     *
     * @param platform the platform to load.
     * @param cause the cause of loading.
     */
    fun load(platform: Platform, cause: Cause)

    /**
     * Enable this plugin for specified platform.
     *
     * For local plugins, it usually means start listening, register commands, etc.
     * Otherwise, it may mean send enable command to remote server.
     *
     * @param platform the platform to enable.
     * @param cause the cause of enabling.
     */
    fun enable(platform: Platform, cause: Cause)

    /**
     * Disable this plugin for specified platform.
     *
     * For local plugins, it usually means stop listening, unregister commands, etc.
     * Otherwise, it may mean send disable command to remote server.
     *
     * Implementations class must ensure that this method will unregister all what
     * plugin registered.
     *
     * @param platform the platform to disable.
     * @param cause the cause of disabling.
     */
    fun disable(platform: Platform, cause: Cause)

    /**
     * Unload this plugin for specified platform.
     *
     * For local plugins, it usually means unload classes, release resources, etc.
     * Otherwise, it may mean disconnect from remote server.
     *
     * @param platform the platform to unload.
     * @param cause the cause of unloading.
     */
    fun unload(platform: Platform, cause: Cause)
}

val AllocatedPlugin.isErrored: Boolean
    get() = runtime.isErrored

val AllocatedPlugin.isEnabled: Boolean
    get() = runtime.isEnabled

val AllocatedPlugin.isLoaded: Boolean
    get() = runtime.isLoaded

val AllocatedPlugin.isNotError: Boolean
    get() = runtime.isNotError

val AllocatedPlugin.isNotEnabled: Boolean
    get() = runtime.isNotEnabled

val AllocatedPlugin.isNotLoaded: Boolean
    get() = runtime.isNotLoaded
