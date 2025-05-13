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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.library.Library
import cn.codethink.xiaoming.library.LibraryDescriptor
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassAccessPolicy
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassPath
import cn.codethink.xiaoming.util.NamespaceId
import java.io.File
import java.net.URI
import java.util.function.Predicate

/**
 * 本地 JVM 经典插件类路径。
 *
 * @author Chuanwise
 */
interface JvmClassicPluginClassPath : JvmPluginClassPath {
    /**
     * 插件依赖库对应的仓库。
     */
    val repositories: List<URI>

    /**
     * 插件依赖库坐标。
     */
    val dependencies: List<LibraryDescriptor>

    /**
     * 插件分发文件。
     */
    val distributionFile: File

    /**
     * 连接到一个库。
     *
     * @param library 要连接的库。
     * @param private 是否私有连接。
     */
    fun link(library: Library, private: Boolean = true)

    /**
     * 连接到一个库。
     *
     * @param uri 要连接的库。
     * @param private 是否私有连接。
     */
    fun link(uri: URI, private: Boolean = true)
}


internal class JvmClassicPluginClassPathImpl(
    override val id: NamespaceId,
    override var classAccessPolicy: JvmPluginClassAccessPolicy,
    override val platform: LocalPlatform?,
    systemClassLoader: ClassLoader,
    publicClassLoader: ClassLoader,
    override var resolveSystemResources: Boolean,
    override var resolvePublicResources: Boolean,
    override var resolveIndependentPluginClasses: Boolean,
    override var allowResolvedByIndependentPlugins: Boolean,
    override val repositories: List<URI>,
    override val dependencies: List<LibraryDescriptor>,
    override val distributionFile: File,
    uniqueResourceFilter: Predicate<String>
) : JvmClassicPluginClassPath {
    override val pluginClassLoader = JvmClassicPluginClassLoader(
        id = id,
        distributionFile = distributionFile,
        systemClassLoader = systemClassLoader,
        publicClassLoader = publicClassLoader,
        platform = platform,
        classPath = this,
        uniqueResourceFilter = uniqueResourceFilter
    )

    override fun link(library: Library, private: Boolean) {
        pluginClassLoader.link(library, private)
    }

    override fun link(uri: URI, private: Boolean) {
        pluginClassLoader.link(uri, private)
    }
}