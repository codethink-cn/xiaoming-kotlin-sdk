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

data class LibraryDescriptorImpl(
    override val group: String,
    override val name: String,
    override val version: String
) : LibraryDescriptor {
    init {
        require(group.isNotEmpty()) { "Group cannot be empty" }
        require(name.isNotEmpty()) { "Name cannot be empty" }
        require(version.isNotEmpty()) { "Version cannot be empty" }
    }

    override fun toString(): String {
        return "$group:$name:$version"
    }
}