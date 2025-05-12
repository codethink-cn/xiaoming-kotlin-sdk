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
import cn.codethink.xiaoming.util.VersionPattern
import cn.codethink.xiaoming.util.toNamespaceId
import cn.codethink.xiaoming.util.toVersion

@Suppress("MemberVisibilityCanBePrivate")
object TestPluginConstants {
    class PluginMetaImpl(
        override val id: NamespaceId,
        override val name: String,
        override val version: Version,
        override val description: String? = null,
        override val standard: VersionPattern? = null,
        override val provisions: List<PluginProvision> = emptyList(),
        override val dependencies: List<PluginDependency> = emptyList()
    ) : PluginMeta {
        override val signature: PluginSignature = PluginSignature(id, version)
    }

    val mc100 = PluginMetaImpl(
        id = "com.example:mc".toNamespaceId(),
        name = "MC",
        version = "1.0.0".toVersion()
    )

    val mc200 = PluginMetaImpl(
        id = "com.example:mc".toNamespaceId(),
        name = "MC",
        version = "2.0.0".toVersion()
    )

    val mcPro100 = PluginMetaImpl(
        id = "com.example:mc-pro".toNamespaceId(),
        name = "MC Pro",
        version = "1.0.0".toVersion(),
        provisions = listOf(
            mc100.toPluginProvision()
        )
    )

    val im100 = PluginMetaImpl(
        id = "com.example:im".toNamespaceId(),
        name = "IM",
        version = "1.0.0".toVersion()
    )

    val imPro100 = PluginMetaImpl(
        id = "com.example:im-pro".toNamespaceId(),
        name = "IM Pro",
        version = "1.0.0".toVersion(),
        provisions = listOf(
            im100.toPluginProvision()
        )
    )

    val mcChat100 = PluginMetaImpl(
        id = "com.example:mc-chat".toNamespaceId(),
        name = "MC Chat",
        version = "1.0.0".toVersion(),
        dependencies = listOf(
            mc100.toPluginDependency()
        )
    )

    val mcChat200 = PluginMetaImpl(
        id = "com.example:mc-chat".toNamespaceId(),
        name = "MC Chat",
        version = "2.0.0".toVersion(),
        dependencies = listOf(
            mc200.toPluginDependency(),
            im100.toPluginDependency()
        )
    )

    val cmi100 = PluginMetaImpl(
        id = "com.example:cmi".toNamespaceId(),
        name = "CMI",
        version = "1.0.0".toVersion(),
        dependencies = listOf(
            mcChat200.toPluginDependency()
        )
    )

    val cmi200 = PluginMetaImpl(
        id = "com.example:cmi".toNamespaceId(),
        name = "CMI",
        version = "2.0.0".toVersion(),
        dependencies = listOf(
            mcChat200.toPluginDependency()
        )
    )
}