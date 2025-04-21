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

@file:JvmName("Strings")

package cn.codethink.xiaoming.util

import org.apache.commons.text.StringEscapeUtils

/**
 * 添加字符串前缀，如果字符串为 null，则返回 null。
 *
 * @author Chuanwise
 */
fun String?.withPrefixOrNull(prefix: String) = this?.let { "$prefix$it" }

/**
 * 添加字符串后缀，如果字符串为 null，则返回 null。
 *
 * @author Chuanwise
 */
fun String?.withSuffixOrNull(suffix: String) = this?.let { "$it$suffix" }

/**
 * 返回一个双引号字符串，并处理其中转义字符。
 *
 * @author Chuanwise
 */
fun String.toDoubleQuotedString(): String = "\"${StringEscapeUtils.unescapeJava(this)}\""

