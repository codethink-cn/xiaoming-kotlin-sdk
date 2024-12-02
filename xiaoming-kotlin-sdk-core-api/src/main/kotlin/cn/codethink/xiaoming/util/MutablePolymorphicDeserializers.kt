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

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * 运行时自由注册和注销类型反序列化器的接口。
 *
 * @author Chuanwise
 * @see PolymorphicDeserializers
 */
interface MutablePolymorphicDeserializers : PolymorphicDeserializers,
    MutableRegistrationManager<JsonDeserializer<*>, Registration<JsonDeserializer<*>>> {
    fun <T> registerNameBasedDeserializer(
        type: Class<T>,
        typeNameField: String,
        typeName: String,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor,
        typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE,
        replacePrevious: Boolean = DEFAULT_REPLACE_PREVIOUS
    ): Boolean

    fun <T> registerTokenBasedDeserializer(
        type: Class<T>,
        token: JsonToken,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor,
        replacePrevious: Boolean = DEFAULT_REPLACE_PREVIOUS
    ): Boolean

    fun <T> registerTypeBasedDeserializer(
        type: Class<in T>,
        targetClass: Class<T>,
        deserializer: JsonDeserializer<out T>,
        subject: SubjectDescriptor
    ): Boolean
}