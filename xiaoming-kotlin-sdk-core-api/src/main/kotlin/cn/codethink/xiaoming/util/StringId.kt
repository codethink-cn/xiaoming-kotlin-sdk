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

package cn.codethink.xiaoming.util

/**
 * 字符串标识符，用于标识某个对象。
 *
 * @author Chuanwise
 */
interface StringId : TextualId, Comparable<StringId> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun of(string: String): StringId = StringId(string)

        @JvmStatic
        @JavaFriendlyApi
        val EMPTY = StringId("")
    }
}
