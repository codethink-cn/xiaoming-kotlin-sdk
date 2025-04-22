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

@file:JvmName("SegmentIdPatternFactory")

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

/**
 * 编译字符串为对应的段 ID 匹配器。
 *
 * BNF 范式:
 *
 * ```bnf
 * SegmentIdPattern := SegmentIdPatternElement | SegmentIdPatternElement "." SegmentIdPatternElement;
 *
 * SegmentIdPatternElement := "+"                      // MinorityRequiredOnceWildcardStringMatcher
 *                       | "?"                      // MinorityOptionalOnceWildcardStringMatcher
 *                       | "++"                     // MajorityRequiredOnceWildcardStringMatcher
 *                       | "??" | "*"               // MajorityOptionalOnceWildcardStringMatcher
 *                       | literal                  // LiteralStringMatcher
 *                       | "{" regex "}"            // RegexStringMatcher
 *                       ;
 * ```
 *
 * @author Chuanwise
 */
@OptIn(InternalApi::class)
@JvmName("createSegmentIdPattern")
fun SegmentIdPattern(pattern: String): SegmentIdPattern {
    return CoreApi.getInstance().createSegmentIdPattern(pattern)
}

@JvmSynthetic
fun String.toSegmentIdPattern(): SegmentIdPattern = SegmentIdPattern(this)

@OptIn(InternalApi::class)
@JvmName("createSegmentIdPattern")
fun SegmentIdPattern(elements: List<SegmentIdPatternElement>): SegmentIdPattern {
    return CoreApi.getInstance().createSegmentIdPattern(elements)
}

@JvmSynthetic
fun List<SegmentIdPatternElement>.toSegmentIdPattern(): SegmentIdPattern = SegmentIdPattern(this)
