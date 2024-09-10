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

import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import org.apache.commons.text.StringSubstitutor

/**
 * Template is a key-value pair, which is used to format a string.
 *
 * @author Chuanwise
 */
class Template(
    raw: Raw
) : AbstractData(raw) {
    val key: String by raw
    val value: String by raw

    @JvmOverloads
    constructor(
        key: String,
        value: String,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MESSAGE_FIELD_KEY] = key
        raw[MESSAGE_FIELD_VALUE] = value
    }
}

fun Template.format(substitutor: StringSubstitutor): String = substitutor.replace(value)
fun Template.format(arguments: Map<String, String>): String = format(StringSubstitutor(arguments))
