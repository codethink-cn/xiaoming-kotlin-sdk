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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class ForwardDeserializer<T>(
    private val type: Class<T>
) : StdDeserializer<T>(type) {
    private val recursive = ThreadLocal<Boolean>()

    override fun deserialize(parser: JsonParser, context: DeserializationContext): T {
        if (recursive.get() != null) {
            throw IllegalStateException("Cannot deserialize object recursively.")
        }
        try {
            recursive.set(true)
            return parser.readValueAs(type)
        } finally {
            recursive.set(null)
        }
    }
}

inline fun <reified T> ForwardDeserializer() = ForwardDeserializer(T::class.java)