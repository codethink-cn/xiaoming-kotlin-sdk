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

@file:JvmName("Reflections")

package cn.codethink.xiaoming.common

import java.lang.reflect.Modifier

private val EMPTY_OBJECT_ARRAY: Array<Any?> = arrayOf()
private val EMPTY_CLASS_ARRAY: Array<Class<*>> = arrayOf()

const val DEFAULT_INSTANCE_GETTER_NAME = "getInstance"
const val DEFAULT_INSTANCE_NAME = "INSTANCE"

@Suppress("UNCHECKED_CAST")
private fun <T> getInstanceOrNull(type: Class<T>): T? {
    // Try call the static method called "getInstance".
    try {
        val instanceGetter = type.getDeclaredMethod(DEFAULT_INSTANCE_GETTER_NAME)
        if (Modifier.isStatic(instanceGetter.modifiers) &&
            instanceGetter.parameters.isEmpty() &&
            type.isAssignableFrom(instanceGetter.returnType)
        ) {

            instanceGetter.trySetAccessible()
            return instanceGetter.invoke(type, *EMPTY_OBJECT_ARRAY) as T
        }
    } catch (ignored: NoSuchMethodException) {
    }

    // Try to get the static field called "INSTANCE".
    try {
        val instance = type.getDeclaredField(DEFAULT_INSTANCE_NAME)
        if (Modifier.isStatic(instance.modifiers) &&
            type.isAssignableFrom(instance.type)
        ) {

            instance.trySetAccessible()
            return instance.get(type) as T
        }
    } catch (ignored: NoSuchFieldException) {
    }

    return null
}

/**
 * Get instance of the given class.
 *
 * It'll first try to get the static field called [DEFAULT_INSTANCE_NAME]
 * in the given class. If failed, try to call
 */
fun <T> getOrConstruct(type: Class<T>): T {
    getInstanceOrNull(type)?.let { return it }

    // Try to call the default constructor.
    try {
        val constructor = type.getDeclaredConstructor(*EMPTY_CLASS_ARRAY)
        constructor.trySetAccessible()

        return constructor.newInstance(*EMPTY_OBJECT_ARRAY)
    } catch (ignored: NoSuchMethodException) {
    }

    throw IllegalStateException(
        "Cannot get instance of $type. " +
                "The type should have an static method called `$DEFAULT_INSTANCE_GETTER_NAME()`, " +
                "have a static field called `$DEFAULT_INSTANCE_NAME, " +
                "or have a default constructor."
    )
}

fun <T> getOrConstruct(type: Class<T>, parameterType: Array<Class<*>>, arguments: Array<*>): T {
    getInstanceOrNull(type)?.let { return it }

    try {
        type.getDeclaredConstructor(*parameterType).let {
            it.trySetAccessible()
            return it.newInstance(*arguments)
        }
    } catch (ignored: NoSuchMethodException) {
    }

    throw IllegalStateException(
        "Cannot get instance of $type. " +
                "The type should have an static method called `$DEFAULT_INSTANCE_GETTER_NAME()`, " +
                "have a static field called `$DEFAULT_INSTANCE_NAME, " +
                "or have a constructor accepts $parameterType (arguments = $arguments)."
    )
}
