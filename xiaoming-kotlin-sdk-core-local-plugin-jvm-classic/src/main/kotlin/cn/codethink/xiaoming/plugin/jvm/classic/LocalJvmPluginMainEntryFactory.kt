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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.common.Cause

class LocalJvmPluginMainEntryContext<T>(
    val cause: Cause,
    val platform: LocalPlatform,
    val classLoader: LocalJvmClassicPluginClassLoader,
    val meta: LocalJvmClassicPluginMeta,
    val mainClass: Class<out T>
)

/**
 * Factory to create [LocalJvmClassicPluginEntry].
 *
 * This interface is used to create [LocalJvmClassicPluginEntry] for all kinds of JVM
 * plugins.
 *
 * @author Chuanwise
 */
interface LocalJvmPluginMainEntryFactory<T> {
    fun create(context: LocalJvmPluginMainEntryContext<T>): LocalJvmClassicPluginEntry
}