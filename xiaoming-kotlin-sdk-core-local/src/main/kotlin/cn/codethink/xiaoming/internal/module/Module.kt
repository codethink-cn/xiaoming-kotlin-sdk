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

package cn.codethink.xiaoming.internal.module

import cn.codethink.xiaoming.LocalPlatformApi
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.ModuleSubject
import cn.codethink.xiaoming.common.Subject

data class ModuleContext(
    val platformApi: LocalPlatformApi,
    val subject: Subject,
    val cause: Cause
)

/**
 * Module is a very special plugins, which must be installed before the platform starts,
 * and uninstall after platform stopped.
 *
 * @author Chuanwise
 */
interface Module {
    val subject: ModuleSubject

    /**
     * Called when the module is installed, platform is start.
     */
    fun onPlatformStart(context: ModuleContext) = Unit

    /**
     * Called when the module is installed, platform is starting.
     */
    fun onPlatformStarting(context: ModuleContext) = Unit

    /**
     * Called when the module is started, platform is started.
     */
    fun onPlatformStarted(context: ModuleContext) = Unit

    /**
     * Called when the module is uninstalled, platform is stopping.
     */
    fun onPlatformStop(context: ModuleContext) = Unit

    /**
     * Called when the module is uninstalled, platform is stopping.
     */
    fun onPlatformStopping(context: ModuleContext) = Unit

    /**
     * Called when the module is uninstalled, platform is stopped.
     */
    fun onPlatformStopped(context: ModuleContext) = Unit
}