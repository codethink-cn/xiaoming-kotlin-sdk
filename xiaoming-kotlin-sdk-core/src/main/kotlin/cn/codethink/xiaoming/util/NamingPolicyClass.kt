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

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import kotlin.reflect.KClass

/**
 * An annotation to specify the naming policy of fields in a class inherited [Data].
 *
 * The reason why we not use [JsonNaming] is that the naming functions in
 * [PropertyNamingStrategy] required a serialization or deserialization config.
 *
 * @author Chuanwise
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FIELD)
annotation class NamingPolicyClass(
    val value: KClass<out NamingPolicy>
)