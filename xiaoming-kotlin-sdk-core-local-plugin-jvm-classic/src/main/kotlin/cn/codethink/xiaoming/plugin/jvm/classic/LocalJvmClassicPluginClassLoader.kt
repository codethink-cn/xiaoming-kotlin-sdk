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

import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.NamespaceId
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.ignoreClassNotFoundException
import cn.codethink.xiaoming.plugin.jvm.DynamicLibrariesClassLoader
import cn.codethink.xiaoming.plugin.jvm.PluginClassAccessPolicy
import io.github.oshai.kotlinlogging.KLogger
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.zip.ZipFile

const val CLASS_FILE_NAME_EXTENSION_WITH_DOT = ".class"

/**
 * Class loader for classic plugins.
 *
 * @param environmentClassLoader Class loader of the environment, such as classes in the package`java.`.
 * @param publicLibrariesClassLoader Class loader of the public libraries.
 * @author Chuanwise
 * @see DynamicLibrariesClassLoader
 */
class LocalJvmClassicPluginClassLoader(
    val id: NamespaceId,
    val distributionFile: File,

    val environmentClassLoader: ClassLoader,
    var resolveEnvironmentResources: Boolean,

    val publicLibrariesClassLoader: ClassLoader,
    var resolvePublicResources: Boolean,

    var classAccessPolicy: PluginClassAccessPolicy,
    var resolveIndependentPluginClasses: Boolean,
    var resolvableByIndependentPlugins: Boolean,

    val ownedResourceNamePrefixes: Set<String>,

    private val pluginClassLoaders: Map<SegmentId, LocalJvmClassicPluginClassLoader>,
    private val logger: KLogger
) : URLClassLoader(
    distributionFile.name, arrayOf(distributionFile.toURI().toURL()), null
) {
    /**
     * Class loaders to load classes in dependent plugins.
     */
    private val dependenciesClassLoaders: Map<SegmentId, LocalJvmClassicPluginClassLoader> = ConcurrentHashMap()

    /**
     * Package names of the classes in the plugin distribution file.
     */
    private val packageNames: Set<String> = distributionFile.filterPackageNames()

    /**
     * Class loader to load classes in protected libraries. For local plugins
     * depended on this plugin, it can use this class loader to load classes or
     * resources.
     */
    private val protectedLibrariesClassLoader = DynamicLibrariesClassLoader(
        environmentClassLoader = environmentClassLoader,
        classLoaderName = "${distributionFile}[protected]",
        toStringName = "ProtectedLibrariesClassLoader(file=${distributionFile})",
        parent = publicLibrariesClassLoader
    )

    /**
     * Class loader to load classes in private libraries. Only this plugin can
     * use this class loader to load classes or resources.
     */
    private val privateLibrariesClassLoader = DynamicLibrariesClassLoader(
        environmentClassLoader = environmentClassLoader,
        classLoaderName = "${distributionFile}[protected]",
        toStringName = "ProtectedLibrariesClassLoader(file=${distributionFile})",
        parent = publicLibrariesClassLoader
    )

    /**
     * Undefined dependencies of this plugin.
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
        ignoreClassNotFoundException { environmentClassLoader.loadClass(name) }?.let { return it }
        ignoreClassNotFoundException { publicLibrariesClassLoader.loadClass(name) }?.let { return it }

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
                if (classLoader.resolvableByIndependentPlugins) {
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
        if (ownedResourceNamePrefixes.any { name.startsWith(it) }) {
            return findResources(name)
        }

        return getResources(name, mutableSetOf())
    }

    override fun getResource(name: String): URL? {
        if (ownedResourceNamePrefixes.any { name.startsWith(it) }) {
            return findResource(name)
        }

        findResource(name)?.let { return it }

        protectedLibrariesClassLoader.getResource(name)?.let { return it }

        dependenciesClassLoaders.values.forEach { classLoader ->
            classLoader.getResource(name)?.let { return it }
        }

        privateLibrariesClassLoader.getResource(name)?.let { return it }

        if (resolvePublicResources) {
            publicLibrariesClassLoader.getResource(name)?.let { return it }
        }

        if (resolveEnvironmentResources) {
            environmentClassLoader.getResource(name)?.let { return it }
        }

        return null
    }

    private fun getResources(
        name: String, trace: MutableSet<ClassLoader>
    ): Enumeration<URL> {
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

        // Find resource from public libraries.
        if (resolvePublicResources) {
            if (!trace.add(publicLibrariesClassLoader)) {
                sources += publicLibrariesClassLoader.getResources(name)
            }
        }

        // Find resource from environment.
        if (resolveEnvironmentResources) {
            if (!trace.add(environmentClassLoader)) {
                sources += environmentClassLoader.getResources(name)
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