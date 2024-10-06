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

package cn.codethink.xiaoming.plugin.jvm

import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.ignoreClassNotFoundException
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.Collections
import java.util.Enumeration
import java.util.concurrent.CopyOnWriteArrayList

private const val JAVA_SYSTEM_CLASS_NAME_PREFIX = "java."
private val EMPTY_URL_ARRAY = arrayOf<URL>()


/**
 * Dynamic classpath class loader.
 *
 * @author Chuanwise
 */
open class DynamicUrlClassLoader(
    urls: Array<URL> = EMPTY_URL_ARRAY, parent: ClassLoader? = null, name: String? = null
) : URLClassLoader(name, urls, parent) {
    init {
        ClassLoader.registerAsParallelCapable()
    }

    fun link(url: URL) = addURL(url)
    fun link(file: File) = addURL(file.toURI().toURL())
}


class DynamicLibrariesClassLoader(
    private val environmentClassLoader: ClassLoader,
    classLoaderName: String,
    private val toStringName: String,
    parent: ClassLoader? = null,
    urls: Array<URL> = EMPTY_URL_ARRAY,
) : DynamicUrlClassLoader(urls, parent, classLoaderName) {
    val libraries: MutableList<DynamicLibrariesClassLoader> = CopyOnWriteArrayList()

    init {
        ClassLoader.registerAsParallelCapable()
    }

    override fun loadClass(name: String): Class<*> {
        ignoreClassNotFoundException { environmentClassLoader.loadClass(name) }?.let { return it }

        loadClassNoEnvironment(name)?.let { return it }

        return generateSequence<ClassLoader>(this) { it.parent }
            .firstOrNull { it !is DynamicLibrariesClassLoader }
            ?.loadClass(name)
            ?: throw ClassNotFoundException(name)
    }

    @InternalApi
    fun loadClassNoEnvironment(
        name: String, trace: MutableSet<DynamicLibrariesClassLoader> = mutableSetOf()
    ): Class<*>? {
        if (name.startsWith(JAVA_SYSTEM_CLASS_NAME_PREFIX)) {
            return null
        }

        // Skip if already loaded.
        if (!trace.add(this)) {
            return null
        }

        // Try parent class loader.
        val parent = this.parent
        if (parent is DynamicLibrariesClassLoader) {
            parent.loadClassNoEnvironment(name, trace)?.let { return it }
        }

        // Try libraries.
        libraries.forEach { library ->
            library.loadClassNoEnvironment(name, trace)?.let { return it }
        }

        // Try self.
        return loadClassInThisClassLoader(name)
    }

    @InternalApi
    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE")
    fun loadClassInThisClassLoader(name: String): Class<*>? {
        return synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return it }
            ignoreClassNotFoundException { findClass(name) }?.let { return it }
        }
    }

    @InternalApi
    fun loadClassInThisClassLoaderAndLibraries(name: String): Class<*>? {
        libraries.forEach { library ->
            library.loadClassInThisClassLoader(name)?.let { return it }
        }
        return loadClassInThisClassLoader(name)
    }

    override fun getResource(name: String): URL? {
        findResource(name)?.let { return it }

        libraries.forEach { classLoader ->
            classLoader.getResource(name)?.let { return it }
        }

        if (parent is DynamicLibrariesClassLoader) {
            parent.getResource(name)?.let { return it }
        }
        return null
    }

    override fun getResources(name: String): Enumeration<URL> {
        var results = findResources(name)

        libraries.forEach { library ->
            results += library.getResources(name)
        }

        return if (parent is DynamicLibrariesClassLoader) {
            results + parent.getResources(name)
        } else results
    }

    @InternalApi
    fun getResources(name: String, trace: MutableSet<ClassLoader>): Enumeration<URL> {
        if (!trace.add(this)) {
            return Collections.emptyEnumeration()
        }
        return getResources(name)
    }

    override fun toString(): String = toStringName
}