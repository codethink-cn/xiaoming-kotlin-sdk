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

package cn.codethink.xiaoming.serialization

import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.Operation
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.Serializers

/**
 * 序列化反序列化处理器解析器。
 *
 * 支持各插件注册类型序列化器，并自动合并针对同一类型的序列化器设置。
 *
 * 获取序列化器时，查找的顺序为：
 *
 * 1. 类型提示（通过 [registerTypeHint] 注册）。
 * 2. 类型序列化处理器（通过 [registerTypeBasedCodec] 注册）。
 * 3. 若存在唯一基于 [JsonToken] 的序列化器，则采用。
 *
 * 获取反序列化器时，查找的顺序为：
 *
 * 1. 类型提示（通过 [registerTypeHint] 注册）。
 * 2. 类型序列化处理器（通过 [registerTypeBasedCodec] 注册）。
 * 3. 解析器尝试读取一个 [JsonToken]，并根据其类型寻找反序列化器（通过 [registerTokenBasedCodec]）。
 * 4. 若为 [JsonToken.START_OBJECT]，尝试读取类型名称字段（通过 [registerNameBasedCodec]）。
 * 5. 若无字段的序列化器，尝试读取类型提示（通过 [registerNameBasedTypeHint]），并从第四步开始向下递归。
 *
 * @author Chuanwise
 */
interface CodecResolver {
    companion object {
        const val DEFAULT_REPLACE = false
        const val DEFAULT_VISIBLE = false
    }

    /**
     * 注册类型提示，将对 [F] 类型的序列化请求转发到 [T] 类型上，适用于某个接口只有一个子类的情况。
     *
     * @param F 原始类型
     * @param T 提示类型
     * @param type 原始类型
     * @param hint 提示类型
     * @param operation 当前操作
     * @param replace 是否替换原有的注册信息
     * @return 注册信息
     */
    fun <F, T : F> registerTypeHint(
        type: Class<F>,
        hint: Class<T>,
        operation: Operation,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<TypeHint<F, T>>?

    /**
     * 注册失败时的类型提示，将对 [F] 类型的序列化请求转发到 [T] 类型上，适用于某个接口只有一个子类的情况。
     *
     * @param F 原始类型
     * @param T 提示类型
     * @param type 原始类型
     * @param hint 提示类型
     * @param operation 当前操作
     * @param replace 是否替换原有的注册信息
     * @return 注册信息
     */
    fun <F, T : F> registerFallbackTypeHint(
        type: Class<F>,
        hint: Class<T>,
        operation: Operation,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<TypeHint<F, T>>?

    /**
     * 注册基于类型名称的序列化处理器。
     *
     * @param T 目标类型
     * @param type 目标类型
     * @param nameField 类型名称字段
     * @param name 类型名称字段的值
     * @param codec 序列化处理器
     * @param visible 类型名称字段是否对序列化处理器可见
     * @param operation 当前操作
     * @param replace 是否替换原有的注册信息
     * @return 注册信息
     */
    fun <T> registerNameBasedCodec(
        type: Class<T>,
        nameField: String,
        name: String,
        codec: Codec<T>,
        operation: Operation,
        visible: Boolean = DEFAULT_VISIBLE,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<Codec<T>>?

    /**
     * 注册基于类型名称的序列化提示。
     *
     * @param F 原始类型
     * @param T 提示类型
     * @param type 原始类型
     * @param nameField 类型名称字段
     * @param name 类型名称字段的值
     * @param hint 提示类型
     * @param visible 类型名称字段是否对序列化处理器可见
     * @param operation 当前操作
     * @param replace 是否替换原有的注册信息
     * @return 注册信息
     */
    fun <F, T : F> registerNameBasedTypeHint(
        type: Class<F>,
        nameField: String,
        name: String,
        hint: Class<T>,
        operation: Operation,
        visible: Boolean = DEFAULT_VISIBLE,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<TypeHint<F, T>>?

    /**
     * 注册基于 [JsonToken] 的序列化处理器。
     *
     * @param T 目标类型
     * @param type 目标类型
     * @param token [JsonToken] 类型
     * @param codec 序列化处理器
     * @param operation 当前操作
     * @return 注册信息
     * @param replace 是否替换原有的注册信息
     */
    fun <T> registerTokenBasedCodec(
        type: Class<T>,
        token: JsonToken,
        codec: Codec<T>,
        operation: Operation,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<Codec<T>>?

    /**
     * 注册基于类型的序列化处理器。
     *
     * @param T 目标类型
     * @param type 目标类型
     * @param codec 序列化处理器
     * @param operation 当前操作
     * @param replace 是否替换原有的注册信息
     * @return 注册信息
     */
    fun <T> registerTypeBasedCodec(
        type: Class<T>,
        codec: Codec<T>,
        operation: Operation,
        replace: Boolean = DEFAULT_REPLACE
    ): MutableRegistration<Codec<T>>?

    /**
     * 将序列化处理器管理器作为 Jackson 模块。
     *
     * @return Jackson 模块
     */
    fun asJacksonModule(): Module

    /**
     * 将序列化处理器管理器作为 Jackson 序列化器。
     *
     * @return Jackson 序列化器
     */
    fun asJacksonSerializers(): Serializers

    /**
     * 将序列化处理器管理器作为 Jackson 反序列化器。
     *
     * @return Jackson 反序列化器
     */
    fun asJacksonDeserializers(): Deserializers

    /**
     * 作废已有的序列化和反序列化器缓存。
     */
    fun invalidateCache()
}