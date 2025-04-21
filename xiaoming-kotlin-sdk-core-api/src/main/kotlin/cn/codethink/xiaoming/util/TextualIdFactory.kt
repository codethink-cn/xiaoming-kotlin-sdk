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

@file:JvmName("TextualIdFactory")

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

/**
 * 解析字符串为 ID。
 *
 * 算法优先将字符串转化为 [NamespaceId]。若失败，尝试转化为 [SegmentId]。否则，使用 [StringId]。
 *
 * @param string 字符串
 * @return ID
 */
@OptIn(InternalApi::class)
@JvmName("parseTextualId")
fun TextualId(string: String): TextualId = CoreApi.getInstance().parseTextualId(string)
