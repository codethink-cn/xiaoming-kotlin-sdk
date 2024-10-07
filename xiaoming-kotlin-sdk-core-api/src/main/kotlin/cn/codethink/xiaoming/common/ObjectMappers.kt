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

@file:JvmName("ObjectMappers")

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Check whether the content of the given value is equal to the given JSON string.
 *
 * @author Chuanwise
 */
@InternalApi
fun ObjectMapper.contentEqual(value: Any?, json: String): Boolean {
    val nodeFromValue = valueToTree<JsonNode>(value)
    val nodeFromJson = readTree(json)
    return nodeFromValue == nodeFromJson
}

/**
 * Assert that the content of the given value is equal to the given JSON string.
 *
 * @author Chuanwise
 */
@InternalApi
fun ObjectMapper.assertJsonContentEquals(expected: String, actual: Any?) {
    if (!contentEqual(actual, expected)) {
        throw AssertionError("Expected: $expected\nActual: ${writeValueAsString(actual)}")
    }
}

/**
 * Assert that the content of the given JSON strings are content equal.
 *
 * @author Chuanwise
 */
@InternalApi
fun ObjectMapper.assertJsonStringContentEquals(expected: String, actual: String) {
    if (readTree(expected) != readTree(actual)) {
        throw AssertionError("Expected: $expected\nActual: $actual")
    }
}