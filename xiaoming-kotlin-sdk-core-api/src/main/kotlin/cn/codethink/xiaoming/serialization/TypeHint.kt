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

/**
 * 序列化类型提示：序列化器或反序列化器查找类型的提示。
 *
 * 例如，反序列化某个类型时，目前反序列化的目标是 `A`。根据注册为 `A` 注册的基于字段的类型提示
 * `"type": "son"`，反序列化器可以调整反序列化目标为 `ASon`，并进一步下探到可用的反序列化器。
 *
 * 而在序列化某一对象时，可以根据类型提示上探到目标对象，获取所有相关的类型提示，以便生成足够的序
 * 列化引导信息。
 *
 * @param F 原始类型
 * @param T 提示类型
 * @author Chuanwise
 */
interface TypeHint<F, T : F> {
    val type: Class<F>
    val hint: Class<T>
}