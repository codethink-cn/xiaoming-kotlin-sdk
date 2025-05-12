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

@file:JvmName("JvmClassicPluginClassPathFactory")

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.library.toLibraryDescriptor
import cn.codethink.xiaoming.plugin.jvm.JvmPluginClassAccessPolicy
import cn.codethink.xiaoming.plugin.jvm.classic.util.createPluginLoggerName
import cn.codethink.xiaoming.plugin.jvm.classic.util.useInputStream
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import java.util.jar.JarFile

private const val MODULE_NAME = "classpath"

internal interface LibrariesData {
    val repositories: List<String>
    val dependencies: List<String>
}

internal class LibrariesDataV1(
    override val repositories: List<String> = emptyList(),
    override val dependencies: List<String> = emptyList()
) : LibrariesData

internal interface AccessData : JvmPluginClassAccessPolicy {
    val resolveSystemResources: Boolean
    val resolveIndependentPluginClasses: Boolean
    val allowResolvedByIndependentPlugins: Boolean

    val accessible: Boolean
    val exceptions: List<String>
}

internal class AccessDataV1(
    override val resolveSystemResources: Boolean = true,
    override val resolveIndependentPluginClasses: Boolean = true,
    override val allowResolvedByIndependentPlugins: Boolean = true,

    override val accessible: Boolean = false,
    override val exceptions: List<String> = emptyList()
) : AccessData {
    override fun isAccessible(name: String): Boolean {
        val matches = exceptions.any { name.startsWith(it) }
        return if (matches) !accessible else accessible
    }
}

@OptIn(InternalApi::class)
fun JvmClassicPluginClassPath(
    id: NamespaceId,
    file: JarFile,
    distributionFile: File,
    platform: LocalPlatform
): JvmClassicPluginClassPath {
    val logger = KotlinLogging.logger(createPluginLoggerName(id, MODULE_NAME))

    val objectMapper = platform.serializationManager.yamlFileObjectMapper
    val librariesData = file.useInputStream(JvmClassicPluginConstants.LIBRARIES_RESOURCE_NAME) {
        objectMapper.readValue<LibrariesData>(it)
    } ?: LibrariesDataV1()

    val accessData = file.useInputStream(JvmClassicPluginConstants.ACCESS_RESOURCE_NAME) {
        objectMapper.readValue<AccessData>(it)
    } ?: AccessDataV1()

    return JvmClassicPluginClassPathImpl(
        id = id,
        classAccessPolicy = accessData,
        platform = platform,
        resolveSystemResources = accessData.resolveSystemResources,
        resolveIndependentPluginClasses = accessData.resolveIndependentPluginClasses,
        allowResolvedByIndependentPlugins = accessData.allowResolvedByIndependentPlugins,
        repositories = librariesData.repositories.map { URI(it) },
        dependencies = librariesData.dependencies.map { it.toLibraryDescriptor() },
        distributionFile = distributionFile,
        uniqueResourceFilter = JvmClassicPluginConstants.UNIQUE_RESOURCE_FILTER,
        logger = logger
    )
}