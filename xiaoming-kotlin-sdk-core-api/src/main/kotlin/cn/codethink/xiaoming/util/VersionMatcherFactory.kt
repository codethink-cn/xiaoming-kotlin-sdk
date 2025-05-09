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

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.api.CoreApi

/**
 * 解析字符串为 [VersionPattern]。
 *
 * 示例：
 *
 * ```
 * 1.0.0                    // Match the exact version.
 * !1.1.3, >=1.0.0, <2.0.0  // Match the version that is not equal to the version,
 *                          // greater than or equal to 1.0.0 and less than 2.0.0.
 * 1.0.+                    // Match the version prefix.
 *
 * 1.0.0                    // Match the exact version.
 * !1.0.0                   // Match the version that is not equal to the version.
 * 1.0.+                    // Match the version prefix.
 * 1.+                      // Match the major version.
 * >1.0.0                   // Greater than version.
 * >=1.0.0                  // Greater than or equal to version.
 * <1.0.0                   // Less than version.
 * <=1.0.0                  // Less than or equal to version.
 * ```
 *
 * BNF 范式：
 *
 * ```bnf
 * versionMatcher := singleVersionPattern               // Match the single version matcher.
 *                 | "(" versionMatcher ")"             // Match the version matcher in parentheses.
 *                 | versionMatcher "&" versionMatcher  // Match the version matcher that is matched by both matchers.
 *                 | versionMatcher "|" versionMatcher  // Match the version matcher that is matched by either matcher.
 *                 ;
 *
 * singleVersionPattern := version                            // Match the exact version.
 *                       | not version                        // Match the version that is not equal to the version.
 *                       | versionRange                       // Match the version range.
 *                       | versionPrefix                      // Match the version prefix.
 *                       ;
 *
 * versionPrefix := digits:major "." digits:minor ".+"  // Match the version prefix.
 *                | digits:major ".+"                   // Match the major version.
 *                ;
 *
 * versionRange := simpleVersionRange;
 *
 * simpleVersionRange := greaterThan version         | version lessThan           // Greater than version.
 *                     | greaterThanOrEqual version  | version lessThanOrEqual    // Greater than or equal to version.
 *                     | lessThan version            | version greaterThan        // Less than version.
 *                     | lessThanOrEqual version     | version greaterThanOrEqual // Less than or equal to version.
 *                     ;
 *
 * not := "!" | "~";
 *
 * greaterThan := ">";
 *
 * greaterThanOrEqual := ">=" | "=>" | "]";
 *
 * lessThan := "<";
 *
 * lessThanOrEqual := "<=" | "" | "]";
 * ```
 *
 * @see VersionPattern
 */
@OptIn(InternalApi::class)
@JvmName("createVersionPattern")
fun VersionPattern(string: String): VersionPattern = CoreApi.getInstance().createVersionPattern(string)

@OptIn(InternalApi::class)
@JvmName("createAndVersionPattern")
fun AndVersionPattern(left: VersionPattern, right: VersionPattern): AndVersionPattern {
    return CoreApi.getInstance().createAndVersionPattern(left, right)
}

@OptIn(InternalApi::class)
@JvmName("OrVersionPattern")
fun OrVersionPattern(left: VersionPattern, right: VersionPattern): OrVersionPattern {
    return CoreApi.getInstance().createOrVersionPattern(left, right)
}

@OptIn(InternalApi::class)
@JvmName("createIncludeVersionPattern")
fun IncludeVersionPattern(value: Version): IncludeVersionPattern {
    return CoreApi.getInstance().createIncludeVersionPattern(value)
}

@OptIn(InternalApi::class)
@JvmName("createExcludeVersionPattern")
fun ExcludeVersionPattern(value: Version): ExcludeVersionPattern {
    return CoreApi.getInstance().createExcludeVersionPattern(value)
}

@OptIn(InternalApi::class)
@JvmName("createGreaterThanVersionPattern")
fun GreaterThanVersionPattern(version: Version): GreaterThanVersionPattern {
    return CoreApi.getInstance().createGreaterThanVersionPattern(version)
}

@OptIn(InternalApi::class)
@JvmName("createGreaterThanOrEqualVersionPattern")
fun GreaterThanOrEqualVersionPattern(version: Version): GreaterThanOrEqualVersionPattern {
    return CoreApi.getInstance().createGreaterThanOrEqualVersionPattern(version)
}

@OptIn(InternalApi::class)
@JvmName("createLessThanVersionPattern")
fun LessThanVersionPattern(version: Version): LessThanVersionPattern {
    return CoreApi.getInstance().createLessThanVersionPattern(version)
}

@OptIn(InternalApi::class)
@JvmName("createLessThanOrEqualVersionPattern")
fun LessThanOrEqualVersionPattern(version: Version): LessThanOrEqualVersionPattern {
    return CoreApi.getInstance().createLessThanOrEqualVersionPattern(version)
}

@OptIn(InternalApi::class)
@JvmName("createMajorVersionPrefixPattern")
fun MajorVersionPrefixPattern(major: Int): MajorVersionPrefixPattern {
    return CoreApi.getInstance().createMajorVersionPrefixPattern(major)
}

@OptIn(InternalApi::class)
@JvmName("createMajorMinorVersionPrefixPattern")
fun MajorMinorVersionPrefixPattern(major: Int, minor: Int): MajorMinorVersionPrefixPattern {
    return CoreApi.getInstance().createMajorMinorVersionPrefixPattern(major, minor)
}
