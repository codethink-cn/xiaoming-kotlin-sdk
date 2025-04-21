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

@file:JvmName("TypeMetaFactory")

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi
import java.lang.reflect.Type
import java.util.function.Supplier
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@OptIn(InternalApi::class)
fun createTypeMeta(type: Type, nullable: Boolean): TypeMeta<*> {
    return CoreApi.getInstance().createTypeMeta(type, nullable)
}

@OptIn(InternalApi::class)
@Suppress("UNCHECKED_CAST")
fun <T> createTypeMeta(type: Class<T>, nullable: Boolean): TypeMeta<T> {
    return CoreApi.getInstance().createTypeMeta(type, nullable) as TypeMeta<T>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> createTypeMeta(): TypeMeta<T> {
    val type = typeOf<T>()
    return createTypeMeta(type.javaType, type.isMarkedNullable) as TypeMeta<T>
}