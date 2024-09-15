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

@file:JvmName("PolymorphicDeserializerServices")

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.Data
import cn.codethink.xiaoming.common.DefaultDataDeserializer
import cn.codethink.xiaoming.common.Subject
import com.fasterxml.jackson.databind.JsonDeserializer
import java.util.ServiceLoader

/**
 * Tool to allow api or core to load polymorphic deserializers as service.
 *
 * For downstream modules want to register their own deserializers to some
 * [PolymorphicDeserializerManager], they should add a resource file named
 * `META-INF/services/cn.codethink.xiaoming.io.data.PolymorphicDeserializerService`
 * and write the full class name of their deserializer service in it.
 *
 * @author Chuanwise
 * @see filterPolymorphicDeserializerServices
 */
interface PolymorphicDeserializerService<T> {
    val type: Class<T>
    val typeName: String
    val deserializer: JsonDeserializer<T>
    val subject: Subject
    val typeNameVisible: Boolean
        get() = DEFAULT_TYPE_NAME_VISIBLE
}

/**
 * @see Data
 * @see DefaultDataDeserializer
 * @see PolymorphicDeserializerService
 */
abstract class DefaultDataPolymorphicDeserializerService<T : Data>(
    override val type: Class<T>,
    override val typeName: String,
    override val subject: Subject,
    override val deserializer: JsonDeserializer<T> = DefaultDataDeserializer(type),
    override val typeNameVisible: Boolean = DEFAULT_TYPE_NAME_VISIBLE
) : PolymorphicDeserializerService<T>

/**
 * Load all polymorphic deserializers of the given type by [ServiceLoader].
 *
 * @author Chuanwise
 */
@Suppress("UNCHECKED_CAST")
fun <T> filterPolymorphicDeserializerServices(type: Class<T>): List<PolymorphicDeserializerService<out T>> {
    return ServiceLoader.load(
        PolymorphicDeserializerService::class.java as Class<PolymorphicDeserializerService<T>>
    ).filter { type.isAssignableFrom(it.type) }.toList()
}