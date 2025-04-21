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

package cn.codethink.xiaoming.util

import java.util.concurrent.locks.ReentrantLock
import java.util.function.Supplier
import kotlin.concurrent.withLock
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

class ReadOnlyStorePropertyImpl<T>(
    private val store: Store,
    private var nameNoLock: String? = null,
    private var metaNoLock: TypeMeta<T>? = null,
    private var namingPolicyNoLock: NamingPolicy? = null,
    private val defaultValueFactory: Supplier<T>? = null
) : ReadOnlyProperty<Any?, T> {
    private var nameBeforeTranslate: String? = null

    private var initialized: Boolean = false
    private val initializeLock = ReentrantLock()

    @OptIn(InternalApi::class)
    @Suppress("UNCHECKED_CAST")
    private fun ensureInitializedByProperty(thisRef: Any?, property: KProperty<*>) {
        if (!initialized) {
            initializeLock.withLock {
                if (!initialized) {
                    nameBeforeTranslate = property.name

                    var namingPolicyLocal = namingPolicyNoLock
                    if (namingPolicyLocal == null) {
                        if (thisRef != null) {
                            for (inheritedClass in thisRef.javaClass.inheritedClasses) {
                                val namingPolicyClass = inheritedClass.getAnnotationsByType(NamingPolicyClass::class.java).singleOrNull() ?: continue

                                namingPolicyLocal = getOrConstruct(namingPolicyClass.value.java)
                                namingPolicyNoLock = namingPolicyLocal
                                break
                            }
                        }
                        check(namingPolicyLocal != null) { "No naming policy is found for property ${property.name}." }
                    }

                    nameNoLock = namingPolicyLocal.translate(property.name)
                    metaNoLock = createTypeMeta(property.returnType.javaType, property.returnType.isMarkedNullable) as TypeMeta<T>

                    initialized = true
                }
            }
        } else {
            check(nameBeforeTranslate == property.name) {
                "The property name is changed from $nameBeforeTranslate to ${property.name}."
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        ensureInitializedByProperty(thisRef, property)
        return store.get(nameNoLock!!, metaNoLock!!, defaultValueFactory)
    }
}