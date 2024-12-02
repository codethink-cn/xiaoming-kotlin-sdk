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
 * 保存数据包的原始数据。
 *
 * 横向上，小明标准允许大家扩展数据包。纵向上，新版小明标准可能增加字段，这都使得可能接收方和发送方
 * 处理的数据格式有所不同。接收方还不能简单地忽略这些字段，因为有些其他功能也许需要这些字段。
 *
 * 因此我们需要用一种松散的存储原始数据的方式，这就是 [Raw]。
 *
 * @author Chuanwise
 */
interface Raw {
    /**
     * 通过给定的类型读取原始数据的字段。
     */
    fun get(
        name: String,
        type: Type,
        optional: Boolean,
        nullable: Boolean,
        convertable: Boolean = false,
        defaultValueFactory: (() -> Any?)? = null
    ): Any?

    /**
     * 设置原始数据的字段。
     */
    operator fun set(name: String, value: Any?)

    operator fun contains(key: String): Boolean

    val keys: Iterable<String>
    val isEmpty: Boolean

    fun contentEquals(raw: Raw): Boolean
    fun contentToString(): String
}