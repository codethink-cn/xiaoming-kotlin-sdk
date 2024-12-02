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
import com.fasterxml.jackson.databind.deser.Deserializers

const val DEFAULT_TYPE_NAME_VISIBLE = true
const val DEFAULT_REPLACE_PREVIOUS = true

/**
 * 运行时自由注册和注销类型反序列化器的接口。
 *
 * 支持三种反序列化方式：基于类型名称字段、基于 [JsonToken] 和基于目标类型。
 *
 * 基于类型名称字段的反序列化策略，常用的场景包括对同一类型（如 [Matcher]）的各种子类型反序列化，
 * 此时常根据 [FIELD_TYPE] 字段的值选择反序列化器。或者根据同一对象的不同版本（如配置文件类），
 * 即根据 [FIELD_VERSION] 字段的值选择反序列化器。
 *
 * 基于 [JsonToken] 的反序列化策略，常用于内联类，例如 [Time] 用毫秒即可构造，故可以直接根据
 * [JsonToken.VALUE_NUMBER_INT] 反序列化。
 *
 * 基于目标类型的反序列化策略，常用于反序列化器的目标类型只有一种实现的情况，在核心模块处常见。
 *
 * 三种序列化方式可以嵌套使用，反序列化器可以被正常构造。例如先根据 [FIELD_TYPE] 找到对应类型，
 * 再根据 [FIELD_VERSION] 找到具体版本。
 *
 * @author Chuanwise
 * @see MutablePolymorphicDeserializers
 */
@InternalImplementedApi
interface PolymorphicDeserializers : Deserializers,
    RegistrationManager<JsonDeserializer<*>, Registration<JsonDeserializer<*>>>