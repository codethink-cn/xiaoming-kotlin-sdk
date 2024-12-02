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

@file:JvmName("SegmentIdMatchers")
@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

/**
 * 段 ID 匹配器。
 *
 * @author Chuanwise
 * @see String.toSegmentIdMatcher
 */
interface SegmentIdMatcher : Matcher<SegmentId> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String): SegmentIdMatcher = string.toSegmentIdMatcher()

        @JvmStatic
        @JavaFriendlyApi
        fun of(strings: List<StringMatcher>): SegmentIdMatcher = strings.toSegmentIdMatcher()

        @JvmStatic
        @JavaFriendlyApi
        fun of(segmentId: SegmentId): SegmentIdMatcher = segmentId.toSegmentIdMatcher()
    }
}

/**
 * 编译字符串为对应的段 ID 匹配器。
 *
 * BNF 范式:
 *
 * ```bnf
 * segmentMatcher := stringMatcher | stringMatcher "." segmentMatcher;
 *
 * stringMatcher := "+"                      // MinorityRequiredOnceWildcardStringMatcher
 *                | "?"                      // MinorityOptionalOnceWildcardStringMatcher
 *                | "++"  | count "++"       // MinorityRequiredWildcardStringMatcher
 *                | "??"  | count "??"       // MinorityOptionalWildcardStringMatcher
 *                | "+++"                    // MajorityRequiredOnceWildcardStringMatcher
 *                | "???" | "*"              // MajorityOptionalOnceWildcardStringMatcher
 *                | literal                  // LiteralStringMatcher
 *                | "{" regex "}"            // RegexStringMatcher
 *                | "\"" escapedLiteral "\"" // LiteralStringMatcher
 *                ;
 * ```
 *
 * @author Chuanwise
 */
fun String.toSegmentIdMatcher(): SegmentIdMatcher = CoreApi.getInstance().parseSegmentIdMatcher(this)
fun List<StringMatcher>.toSegmentIdMatcher(): SegmentIdMatcher = CoreApi.getInstance().createSegmentIdMatcher(this)
fun SegmentId.toSegmentIdMatcher(): SegmentIdMatcher = CoreApi.getInstance().createSegmentIdMatcher(this)