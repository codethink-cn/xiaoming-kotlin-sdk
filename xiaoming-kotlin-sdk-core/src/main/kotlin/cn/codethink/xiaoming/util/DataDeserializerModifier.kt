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

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier


/**
 * 标注一个 [JsonDeserializer] 的子类型是 [Data] 子类型的特殊反序列化器，避免被覆盖为默认反序列化器。
 *
 * @author Chuanwise
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DataDeserializer

/**
 * 反序列化器编辑器，覆盖 [Data] 子类型的反序列化器为 [DefaultDataDeserializer]。
 *
 * 如果反序列化器类型带有 [DataDeserializer] 注解，将会被原样使用而不会被覆盖。
 *
 * @author Chuanwise
 */
object DataDeserializerModifier : BeanDeserializerModifier() {
    private fun readResolve(): Any = DataDeserializerModifier

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        if (!beanDesc.type.isTypeOrSubTypeOf(Data::class.java)) {
            return super.modifyDeserializer(config, beanDesc, deserializer)
        }

        if (deserializer.javaClass.getAnnotation(DataDeserializer::class.java) != null) {
            return deserializer
        }

        @Suppress("UNCHECKED_CAST")
        return DefaultDataDeserializer(beanDesc.type.rawClass as Class<Data>)
    }
}

inline fun <reified T : Data> DefaultDataDeserializer() = DefaultDataDeserializer(T::class.java)

