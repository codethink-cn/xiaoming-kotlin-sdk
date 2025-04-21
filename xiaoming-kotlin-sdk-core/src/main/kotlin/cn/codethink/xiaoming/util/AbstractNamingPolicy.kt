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

import com.fasterxml.jackson.databind.PropertyNamingStrategies

/**
 * @author Chuanwise
 * @see PropertyNamingStrategies
 * @see NamingPolicyClass
 */
abstract class AbstractNamingPolicy(
    private val base: PropertyNamingStrategies.NamingBase? = null
) : NamingPolicy {
    override fun translate(name: String): String = base?.translate(name) ?: name
}

object NoNamingPolicy : AbstractNamingPolicy()

object KebabCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.KebabCaseStrategy.INSTANCE)
object LowerCamelCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.LowerCamelCaseStrategy.INSTANCE)
object UpperCamelCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE)
object SnakeCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE)
object UpperSnakeCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE)
object LowerCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.LowerCaseStrategy.INSTANCE)
object LowerDotCaseNamingPolicy : AbstractNamingPolicy(PropertyNamingStrategies.LowerDotCaseStrategy.INSTANCE)