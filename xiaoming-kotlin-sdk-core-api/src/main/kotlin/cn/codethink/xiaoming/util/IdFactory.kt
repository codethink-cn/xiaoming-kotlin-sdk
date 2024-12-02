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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

/**
 * 解析字符串为 ID。
 *
 * 算法优先将字符串转化为 [NamespaceId]。若失败，尝试转化为 [SegmentId]。否则，使用 [StringId]。
 *
 * @param string 字符串
 * @see NamespaceId
 * @see SegmentId
 * @see StringId
 */
fun parseId(string: String): Id = CoreApi.getInstance().parseId(string)

fun createNamespaceId(group: SegmentId, name: String) = CoreApi.getInstance().createNamespaceId(group, name)
fun createNamespaceId(group: SegmentId, name: SegmentId) = CoreApi.getInstance().createNamespaceId(group, name)
fun parseNamespaceId(string: String) = CoreApi.getInstance().parseNamespaceId(string)

fun createStringId(string: String): StringId = CoreApi.getInstance().createStringId(string)

fun createNumericalId(number: Int): NumericalId = CoreApi.getInstance().createNumericalId(number)
fun createNumericalId(number: Long): NumericalId = CoreApi.getInstance().createNumericalId(number)

fun createSegmentId(segments: List<String>): SegmentId = CoreApi.getInstance().createSegmentId(segments)
fun parseSegmentId(string: String): SegmentId = CoreApi.getInstance().parseSegmentId(string)