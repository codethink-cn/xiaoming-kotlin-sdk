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

package cn.codethink.xiaoming.serialization

import cn.codethink.xiaoming.util.Operation

/**
 * 初始化 [CodecResolver] 的上下文。
 *
 * @author Chuanwise
 */
interface CodecResolverInitializeContext {
    val resolver: CodecResolver
    val operation: Operation
    val replace: Boolean
    val visible: Boolean
}