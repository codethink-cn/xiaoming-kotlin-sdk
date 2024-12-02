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

package cn.codethink.xiaoming.util

import java.util.Objects
import java.util.Properties

/**
 * 表示一个字符串模板。
 *
 * 字符串模板中可以使用 `${` 和 `}` 包围变量。
 *
 * @author Chuanwise
 * @see parseTemplate
 */
interface Template {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(format: String): Template = parseTemplate(format)
    }

    fun format(mapper: (String) -> String): String

    override fun toString(): String
}

fun String.toTemplate(): Template = parseTemplate(this)

fun Template.format(map: Map<String, *>): String = format { map[it].toString() }
fun Template.format(properties: Properties): String = format { Objects.toString(properties[it]) }
fun Template.format(pair: Pair<String, *>): String = format(mapOf(pair))
fun Template.format(vararg pairs: Pair<String, *>): String = format(mapOf(*pairs))
fun Template.format(raw: Raw): String = format { raw.getAsStringOrDefault(it, it) }