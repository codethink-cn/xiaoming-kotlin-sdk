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

import java.lang.reflect.Type

/**
 * 表示一种类型。
 *
 * @author Chuanwise
 * @see createTypeMeta
 */
@NotStableForInheritance
interface TypeMeta<T> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun of(type: Type, nullable: Boolean = false): TypeMeta<*> = createTypeMeta(type, nullable)

        @JvmStatic
        @JavaFriendlyApi
        fun <T> of(type: Class<T>, nullable: Boolean = false): TypeMeta<T> = createTypeMeta(type, nullable)
    }

    val type: Type
    val nullable: Boolean
}