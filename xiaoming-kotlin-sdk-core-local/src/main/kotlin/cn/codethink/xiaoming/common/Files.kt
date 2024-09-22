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

@file:JvmName("Files")

package cn.codethink.xiaoming.common

import java.io.File

/**
 * Make sure a directory exists.
 *
 * @throws IllegalStateException if the directory does not exist and failed to create it.
 */
@JvmOverloads
fun File.ensureExistedDirectory(
    block: () -> String = { "Failed to create directory ${absolutePath}." }
) {
    if (!isDirectory && !mkdirs()) {
        throw IllegalStateException(block())
    }
}

const val LANGUAGE_RESOURCE_DIRECTORY_PATH = "xiaoming/languages"
const val DEFAULT_LOCALE_LANGUAGE_RESOURCE_DIRECTORY_PATH = "$LANGUAGE_RESOURCE_DIRECTORY_PATH/en_US"