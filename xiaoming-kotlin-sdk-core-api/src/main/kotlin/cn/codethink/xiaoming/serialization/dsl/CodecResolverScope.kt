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

import cn.codethink.xiaoming.serialization.CodecResolver
import cn.codethink.xiaoming.serialization.CodecResolverInitializeContext
import cn.codethink.xiaoming.serialization.TypeHint
import cn.codethink.xiaoming.util.MutableRegistration
import cn.codethink.xiaoming.util.Operation

class CodecResolverScope(
    val resolver: CodecResolver,
    val operation: Operation,
    val replace: Boolean,
    val visible: Boolean
)

inline fun CodecResolver.registering(
    operation: Operation,
    replace: Boolean = CodecResolver.DEFAULT_REPLACE,
    visible: Boolean = CodecResolver.DEFAULT_VISIBLE,
    block: CodecResolverScope.() -> Unit
) {
    CodecResolverScope(this, operation, replace, visible).block()
}

inline fun CodecResolverInitializeContext.registering(
    operation: Operation = this.operation,
    replace: Boolean = this.replace,
    visible: Boolean = this.visible,
    block: CodecResolverScope.() -> Unit
) {
    CodecResolverScope(resolver, operation, replace, visible).block()
}

inline fun <reified T, reified U : T> CodecResolverScope.hint(
    replace: Boolean = this.replace,
    operation: Operation = this.operation
): MutableRegistration<TypeHint<T, U>>? {
    return resolver.registerTypeHint(
        type = T::class.java,
        hint = U::class.java,
        operation = operation,
        replace = replace
    )
}