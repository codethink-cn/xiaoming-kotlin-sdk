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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.classpath.DynamicLibrariesClassLoader
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.SegmentIdImpl
import cn.codethink.xiaoming.util.ignoreClassNotFoundException
import cn.codethink.xiaoming.plugin.jvm.LocalJvmPluginClassAccessPolicy
import io.github.oshai.kotlinlogging.KLogger
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Predicate
import java.util.zip.ZipFile

const val CLASS_FILE_NAME_EXTENSION_WITH_DOT = ".class"

class LocalJvmClassicPluginClassLoader(
    private var id: NamespaceId,
    override val distributionFile: File,

    override var resolvePlatformResources: Boolean,
    override var resolveEnvironmentResources: Boolean,

    override var classAccessPolicy: LocalJvmPluginClassAccessPolicy,
    override var resolveIndependentPluginClasses: Boolean,
    override var allowResolvedByIndependentPlugins: Boolean,

    private val uniqueResourcesFilter: Predicate<String>,
    private var pluginClassLoaders: Map<NamespaceId, LocalJvmClassicPluginClassLoader>,
    private var logger: KLogger,
    platform: LocalPlatform
) : URLClassLoader(
    distributionFile.name, arrayOf(distributionFile.toURI().toURL()), null
), LocalJvmClassicPluginClassPath {
    private val platformClassLoader by platform.pluginManager::platformClassLoader
    private val environmentClassLoader by platform.pluginManager::environmentClassLoader

    override val pluginClassLoader: ClassLoader = this

    /**
     * 用于加载依赖插件的类加载器。
     */
    private val dependenciesClassLoaders: Map<NamespaceId, LocalJvmClassicPluginClassLoader> = ConcurrentHashMap()

    /**
     * 插件分发文件内的包名。
     */
    private val packageNames: Set<String> = distributionFile.filterPackageNames()

    /**
     * 保护类加载器。依赖于此插件的其他本地插件也可以使用。
     */
    private val protectedLibrariesClassLoader = DynamicLibrariesClassLoader(
        environmentClassLoader = environmentClassLoader,
        classLoaderName = "${distributionFile}[protected]",
        toStringName = "ProtectedLibrariesClassLoader(file=${distributionFile})",
        parent = platformClassLoader
    )

    /**
     * 私有类加载器，只有插件自身可以使用。
     */
    private val privateLibrariesClassLoader = DynamicLibrariesClassLoader(
        environmentClassLoader = environmentClassLoader,
        classLoaderName = "${distributionFile}[private]",
        toStringName = "ProtectedLibrariesClassLoader(file=${distributionFile})",
        parent = platformClassLoader
    )

    /**
     * 未定义的插件依赖。当插件尚未声明其依赖关系，但却使用其中的类时维护。
     */
    private val undefinedDependencies: MutableSet<NamespaceId> = CopyOnWriteArraySet()

    private fun resolveProtectedLibrariesAndPublicClass(name: String): Class<*>? {
        ignoreClassNotFoundException {
            protectedLibrariesClassLoader.loadClassNoEnvironment(name)
        }?.let { return it }
        return resolvePublicClass(name)
    }

    private fun resolvePublicClass(name: String): Class<*>? {
        val packageName = name.classNameToPackageName()
        if (!packageNames.contains(packageName)) {
            return null
        }
        if (!classAccessPolicy.isAccessible(name)) {
            return null
        }
        return loadClassInThisClassLoader(name)
    }

    fun link(uri: URI, private: Boolean) {
        if (private) {
            logger.trace { "Linking private library: $uri." }
            privateLibrariesClassLoader.link(uri.toURL())
        } else {
            logger.trace { "Linking private library: $uri." }
            protectedLibrariesClassLoader.link(uri.toURL())
        }
        logger.debug { "Linked library: $uri." }
    }

    fun link(classLoader: DynamicLibrariesClassLoader, private: Boolean) {
        if (private) {
            logger.debug { "Linking private library: $classLoader." }
            privateLibrariesClassLoader.libraries.add(classLoader)
        } else {
            logger.debug { "Linking protected library: $classLoader." }
            protectedLibrariesClassLoader.libraries.add(classLoader)
        }
    }

    @InternalApi
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE")
    fun loadClassInThisClassLoader(name: String): Class<*>? {
        return synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return it }
            ignoreClassNotFoundException { findClass(name) }?.let { return it }
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> = loadClass(name)

    override fun loadClass(name: String): Class<*> {
        ignoreClassNotFoundException { platformClassLoader.loadClass(name) }?.let { return it }
        ignoreClassNotFoundException { environmentClassLoader.loadClass(name) }?.let { return it }

        // Load class in protected libraries.
        protectedLibrariesClassLoader.loadClassInThisClassLoaderAndLibraries(name)?.let { return it }

        // Find class in dependencies.
        dependenciesClassLoaders.values.forEach { classLoader ->
            classLoader.resolveProtectedLibrariesAndPublicClass(name)?.let { return it }
        }

        // Load class in private libraries.
        privateLibrariesClassLoader.loadClassInThisClassLoaderAndLibraries(name)?.let { return it }

        // Load by this class loader.
        loadClassInThisClassLoader(name)?.let { return it }

        val resolveIndependentPluginClasses = resolveIndependentPluginClasses
        pluginClassLoaders.forEach { (id, classLoader) ->
            if (classLoader != this && !dependenciesClassLoaders.containsKey(id)) {
                if (classLoader.allowResolvedByIndependentPlugins) {
                    classLoader.resolveProtectedLibrariesAndPublicClass(name)?.let {
                        if (undefinedDependencies.add(classLoader.id)) {
                            logger.warn {
                                "Plugin '${id}' (${distributionFile}) class $name " +
                                        "of '${classLoader.id}' (${classLoader.distributionFile.name}) but not depend on it. "
                            }

                            if (resolveIndependentPluginClasses) {
                                return it
                            } else {
                                return@forEach
                            }
                        }
                        return it
                    }
                }
            }
        }

        throw ClassNotFoundException(name)
    }

    override fun getResources(name: String): Enumeration<URL> {
        if (uniqueResourcesFilter.test(name)) {
            return findResources(name)
        }

        return getResources(name, mutableSetOf())
    }

    override fun getResource(name: String): URL? {
        if (uniqueResourcesFilter.test(name)) {
            return findResource(name)
        }

        findResource(name)?.let { return it }

        protectedLibrariesClassLoader.getResource(name)?.let { return it }

        dependenciesClassLoaders.values.forEach { classLoader ->
            classLoader.getResource(name)?.let { return it }
        }

        privateLibrariesClassLoader.getResource(name)?.let { return it }

        if (resolveEnvironmentResources) {
            environmentClassLoader.getResource(name)?.let { return it }
        }

        if (resolvePlatformResources) {
            platformClassLoader.getResource(name)?.let { return it }
        }

        return null
    }

    private fun getResources(name: String, trace: MutableSet<ClassLoader>): Enumeration<URL> {
        if (!trace.add(this)) {
            return Collections.emptyEnumeration()
        }
        val sources = mutableListOf(findResources(name))

        // Find resource from protected libraries.
        sources += protectedLibrariesClassLoader.getResources(name, trace)

        // Find resource from dependencies.
        dependenciesClassLoaders.values.forEach { classLoader ->
            sources += classLoader.getResources(name, trace)
        }

        // Find resource from private libraries.
        sources += privateLibrariesClassLoader.getResources(name, trace)

        // Find resource from environment class loader.
        if (resolveEnvironmentResources) {
            if (!trace.add(environmentClassLoader)) {
                sources += environmentClassLoader.getResources(name)
            }
        }

        // Find resource from platform class loader.
        if (resolvePlatformResources) {
            if (!trace.add(platformClassLoader)) {
                sources += platformClassLoader.getResources(name)
            }
        }

        val resolved = sources.flatMap { it.toList() }.toSet()
        return Collections.enumeration(resolved)
    }

    override fun toString(): String = "LocalJvmClassicPluginClassLoader(file=${distributionFile})"
}

private fun String.classNameToPackageName(): String {
    return substringBeforeLast('.')
}

private fun File.filterPackageNames(): Set<String> {
    return ZipFile(this).use { file ->
        file.entries().asSequence()
            .filter { it.name.endsWith(CLASS_FILE_NAME_EXTENSION_WITH_DOT) }
            .map { it.name.substringBeforeLast('.') }
            .map { it.removePrefix("/") }
            .map { it.replace('/', '.') }
            .map { it.classNameToPackageName() }
            .toSet()
    }
}