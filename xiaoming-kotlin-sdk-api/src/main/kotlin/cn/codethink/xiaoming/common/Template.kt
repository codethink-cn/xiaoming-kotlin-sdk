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

@file:JvmName("Templates")

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.apache.commons.text.StringSubstitutor

/**
 * Template is a key-value pair, which is used to format a string.
 *
 * @author Chuanwise
 */
@JvmInline
@JsonSerialize(using = TemplateSerializer::class)
@JsonDeserialize(using = TemplateDeserializer::class)
value class Template(
    private val format: String
) {
    fun format(substitutor: StringSubstitutor): String = substitutor.replace(format)
    fun format(arguments: Map<String, Any?>): String = format(StringSubstitutor(arguments))

    override fun toString(): String = format
}

object TemplateSerializer : StdSerializer<Template>(Template::class.java) {
    private fun readResolve(): Any = TemplateSerializer
    override fun serialize(template: Template, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(template.toString())
    }
}

object TemplateDeserializer : StdDeserializer<Template>(Template::class.java) {
    private fun readResolve(): Any = TemplateDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Template {
        return Template(parser.text)
    }
}
