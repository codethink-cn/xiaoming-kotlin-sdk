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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * It's not recommend to save password on [String] because we
 * can not control the life cycle of [String] in JVM. So this
 * class is used to store password in a more secure way. After
 * used, call [invalidate] to make sure the password content
 * has been cleared in JVM.
 *
 * @author Chuanwise
 */
@JsonSerialize(using = PasswordSerializer::class)
@JsonDeserialize(using = PasswordDeserializer::class)
class Password(
    chars: CharArray
) {
    @Volatile
    private var chars: CharArray? = chars

    val isInvalid: Boolean
        get() = chars == null

    val isValid: Boolean
        get() = !isInvalid

    fun invalidate() {
        chars = null
    }

    fun toStringUnsafe(): String {
        val chars = chars
        assertValid(chars)
        return String(chars!!)
    }

    fun toCharArray(): CharArray {
        val chars = chars
        assertValid(chars)
        return chars!!
    }

    private fun assertValid(chars: CharArray?) {
        if (chars == null) {
            throw IllegalStateException("Password has already been invalidated.")
        }
    }
}

object PasswordSerializer : StdSerializer<Password>(Password::class.java) {
    private fun readResolve(): Any = PasswordSerializer
    override fun serialize(password: Password, generator: JsonGenerator, serializerProvider: SerializerProvider) {
        val charArray = password.toCharArray()
        generator.writeString(charArray, 0, charArray.size)
    }
}

object PasswordDeserializer : StdDeserializer<Password>(Password::class.java) {
    private fun readResolve(): Any = PasswordDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Password {
        return Password(parser.textCharacters)
    }
}