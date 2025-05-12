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

import java.util.function.Predicate

@Suppress("MemberVisibilityCanBePrivate")
object JvmClassicPluginConstants {
    const val PLUGIN_RESOURCE_NAME = "plugin.yml"
    const val LIBRARIES_RESOURCE_NAME = "libraries.yml"
    const val ACCESS_RESOURCE_NAME = "access.yml"

    internal val UNIQUE_RESOURCE_NAMES = setOf(
        PLUGIN_RESOURCE_NAME,
        LIBRARIES_RESOURCE_NAME,
        ACCESS_RESOURCE_NAME,
    )
    internal val UNIQUE_RESOURCE_FILTER = Predicate<String> { name ->
        UNIQUE_RESOURCE_NAMES.contains(name)
    }
}