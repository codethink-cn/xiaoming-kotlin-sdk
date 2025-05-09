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

package cn.codethink.xiaoming.library

interface LibraryManager {
    /**
     * 系统类加载器，加载 Java 和小明系统等系统类。
     */
    val systemClassLoader: ClassLoader

    /**
     * 公共库加载器，插件之间共享的公共库。
     */
    val publicClassLoader: ClassLoader

    /**
     * 已经加载的公共库。
     */
    val publicLibraries: List<Library>
}