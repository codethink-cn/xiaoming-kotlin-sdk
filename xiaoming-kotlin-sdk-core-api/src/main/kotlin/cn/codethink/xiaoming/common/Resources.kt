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

package cn.codethink.xiaoming.common

import java.io.InputStream

@InternalApi
fun Any.getTestResourceAsStream(path: String): InputStream {
    // 1. Get resource as stream.
    this::class.java.classLoader.getResourceAsStream(path)?.let { return it }

    // 2. Get example resource as stream.
    val examplePath = "$path.example"
    this::class.java.classLoader.getResourceAsStream(examplePath)?.let {
        throw NoSuchElementException("Test resource not found: '$path'. Copy and modify example file: '$examplePath' first.")
    }

    throw NoSuchElementException("Test resource not found: '$path'.")
}