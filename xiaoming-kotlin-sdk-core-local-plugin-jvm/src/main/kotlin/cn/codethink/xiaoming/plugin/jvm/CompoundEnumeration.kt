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

package cn.codethink.xiaoming.plugin.jvm

import java.util.Enumeration

class CompoundEnumeration<T>(
    private val first: Enumeration<T>,
    private val second: Enumeration<T>
) : Enumeration<T> {
    override fun hasMoreElements(): Boolean {
        return first.hasMoreElements() || second.hasMoreElements()
    }

    override fun nextElement(): T {
        return if (first.hasMoreElements()) {
            first.nextElement()
        } else {
            second.nextElement()
        }
    }
}

operator fun <T> Enumeration<T>.plus(other: Enumeration<T>): Enumeration<T> = CompoundEnumeration(this, other)
