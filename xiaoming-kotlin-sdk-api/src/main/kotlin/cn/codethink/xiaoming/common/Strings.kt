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

package cn.codethink.xiaoming.common

/**
 * Return the string with the given prefix or null if the string is null.
 *
 * @author Chuanwise
 */
fun String?.prependOrNull(prefix: String) = this?.let { "$prefix$it" }

/**
 * Return the string with the given suffix or null if the string is null.
 *
 * @author Chuanwise
 */
fun String?.appendOrNull(suffix: String) = this?.let { "$it$suffix" }

