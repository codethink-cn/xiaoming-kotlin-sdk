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

import cn.codethink.xiaoming.api.CoreApi
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

inline operator fun <reified T : Any?> Store.get(name: String): T = get(name, createTypeMeta<T>())

@OptIn(InternalApi::class)
inline fun <reified T> Store.property(
    name: String? = null,
    meta: TypeMeta<T>? = createTypeMeta(),
    namingPolicy: NamingPolicy? = null,
    defaultValueFactory: Supplier<T>? = null
): ReadOnlyProperty<Any?, T> {
    return CoreApi.getInstance().createReadOnlyStoreProperty(this, name, meta, namingPolicy, defaultValueFactory)
}

@OptIn(InternalApi::class)
inline fun <reified T> MutableStore.property(
    name: String? = null,
    meta: TypeMeta<T>? = createTypeMeta(),
    namingPolicy: NamingPolicy? = null,
    defaultValueFactory: Supplier<T>? = null
): ReadWriteProperty<Any?, T> {
    return CoreApi.getInstance().createReadWriteStoreProperty(this, name, meta, namingPolicy, defaultValueFactory)
}
