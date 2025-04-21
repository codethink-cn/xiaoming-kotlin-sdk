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


/**
 * 表示一个数据元素，内部使用 [Store] 表示原始数据。
 *
 * 序列化时，只会序列化 [Store] 内的数据。
 *
 * 实现类如果不是单例，有一个标注了 [InternalApi] 且只有一个 [Store] 参数的构造函数，
 * 则反序列化时将调用这个构造函数并传入原始数据。否则将会在强制构造后设置 [Store]。
 *
 * @author Chuanwise
 * @see Store
 */
interface Data {
    val raw: Store
}
