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

import cn.codethink.xiaoming.api.CoreApi

/**
 * 标识符命名策略。
 *
 * @author Chuanwise
 */
interface NamingPolicy {
    @OptIn(InternalApi::class)
    companion object {
        @JvmStatic
        val KEBAB_CASE = CoreApi.getInstance().getKebabCaseNamingPolicy()

        val LOWER_CAMEL_CASE = CoreApi.getInstance().getLowerCamelCaseNamingPolicy()

        val UPPER_CAMEL_CASE = CoreApi.getInstance().getUpperCamelCaseNamingPolicy()
        val SNAKE_CASE = CoreApi.getInstance().getSnakeCaseNamingPolicy()
        val UPPER_SNAKE_CASE = CoreApi.getInstance().getUpperSnakeCaseNamingPolicy()
        val LOWER_CASE = CoreApi.getInstance().getLowerCaseNamingPolicy()
        val LOWER_DOT_CASE = CoreApi.getInstance().getLowerDotCaseNamingPolicy()
    }

    fun translate(name: String): String
}