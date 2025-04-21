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

package cn.codethink.xiaoming.serialization.dsl

import cn.codethink.xiaoming.serialization.Codec
import cn.codethink.xiaoming.serialization.TypeHint
import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.util.FIELD_VERSION
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.Operation

class CodecResolverTypeFieldScope<T>(
    val outerScope: CodecResolverScope,
    val rawClass: Class<T>,
    val nameField: String
)

inline fun <reified T> CodecResolverScope.typeField(block: CodecResolverTypeFieldScope<T>.() -> Unit) {
    type<T> {
        typeField(block)
    }
}

inline fun <reified T> CodecResolverScope.versionField(block: CodecResolverTypeFieldScope<T>.() -> Unit) {
    type<T> {
        versionField(block)
    }
}

inline fun <T> CodecResolverTypeScope<T>.field(nameField: String, block: CodecResolverTypeFieldScope<T>.() -> Unit) {
    CodecResolverTypeFieldScope(outerScope, rawClass, nameField).block()
}

inline fun <T> CodecResolverTypeScope<T>.typeField(block: CodecResolverTypeFieldScope<T>.() -> Unit) {
    field(FIELD_TYPE, block)
}

inline fun <T> CodecResolverTypeScope<T>.versionField(block: CodecResolverTypeFieldScope<T>.() -> Unit) {
    field(FIELD_VERSION, block)
}

inline fun <reified T> CodecResolverTypeFieldScope<in T>.hint(
    name: String,
    visible: Boolean = outerScope.visible,
    replace: Boolean = outerScope.replace,
    operation: Operation = outerScope.operation,
    block: (CodecResolverTypeScope<T>.() -> Unit) = {}
): MutableRegistration<TypeHint<in T, T>>? {
    return outerScope.resolver.registerNameBasedTypeHint(
        type = rawClass,
        nameField = nameField,
        name = name,
        hint = T::class.java,
        operation = operation,
        visible = visible,
        replace = replace
    ).apply {
        outerScope.type<T>(block)
    }
}

fun <T> CodecResolverTypeFieldScope<T>.handler(
    name: String,
    handler: Codec<T>,
    visible: Boolean
): MutableRegistration<Codec<T>>? {
    return outerScope.resolver.registerNameBasedCodec(
        type = rawClass,
        nameField = nameField,
        name = name,
        codec = handler,
        operation = outerScope.operation,
        visible = visible,
        replace = outerScope.replace
    )
}
