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
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.Subject

enum class PluginState {
    INITIALIZED,
    LOADING,
    LOADING_ERROR,
    LOADED,
    ENABLING,
    ENABLING_ERROR,
    ENABLED,
    DISABLING,
    DISABLING_ERROR,
    DISABLED,
}

/**
 * Plugin is a set of functions that extend platform's abilities. It can service
 * platform locally and remotely.
 *
 * @author Chuanwise
 */
interface Plugin {
    val state: PluginState
    val subject: PluginSubject

    fun load(platform: Platform, cause: Cause, subject: Subject)
    fun enable(platform: Platform, cause: Cause, subject: Subject)
    fun disable(platform: Platform, cause: Cause, subject: Subject)
    fun unload(platform: Platform, cause: Cause, subject: Subject)
}