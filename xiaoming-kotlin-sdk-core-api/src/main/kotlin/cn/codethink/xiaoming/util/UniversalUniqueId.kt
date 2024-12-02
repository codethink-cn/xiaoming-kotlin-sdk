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

@file:JvmName("UniversalUniqueIds")

package cn.codethink.xiaoming.util

import java.util.UUID

/**
 * 通过 [UUID] 实现的 [Id]。
 *
 * @author Chuanwise
 * @see createRandomUniversalUniqueId
 * @see createUniversalUniqueId
 */
interface UniversalUniqueId : Id, Comparable<UniversalUniqueId> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun random(): UniversalUniqueId = createRandomUniversalUniqueId()

        @JvmStatic
        @JavaFriendlyApi
        fun of(uuid: UUID): UniversalUniqueId = createUniversalUniqueId(uuid)
    }

    fun toUuid(): UUID
}

fun UUID.toUniversalUniqueId(): UniversalUniqueId = createUniversalUniqueId(this)