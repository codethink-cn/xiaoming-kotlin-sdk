/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version

class PluginSignatureImpl(
    override val id: NamespaceId,
    override val version: Version
) : PluginSignature {
    private val toPairCache = Pair(id, version)

    override fun toPair(): Pair<NamespaceId, Version> {
        return toPairCache
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginSignatureImpl

        if (id != other.id) return false
        return version == other.version
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    override fun toString(): String {
        return "$id:$version"
    }
}