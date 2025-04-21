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

import cn.codethink.xiaoming.api.CoreApi

/**
 * 解析字符串为 [VersionMatcher]。
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
 * versionMatcher := singleVersionMatcher               // Match the single version matcher.
 *                 | "(" versionMatcher ")"             // Match the version matcher in parentheses.
 *                 | versionMatcher "&" versionMatcher  // Match the version matcher that is matched by both matchers.
 *                 | versionMatcher "|" versionMatcher  // Match the version matcher that is matched by either matcher.
 *                 ;
 *
 * singleVersionMatcher := version                            // Match the exact version.
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
 * @see VersionMatcher
 */
@OptIn(InternalApi::class)
@JvmName("createVersionMatcher")
fun VersionMatcher(string: String): VersionMatcher = CoreApi.getInstance().parseVersionMatcher(string)

@OptIn(InternalApi::class)
@JvmName("createAndVersionMatcher")
fun AndVersionMatcher(left: VersionMatcher, right: VersionMatcher): AndVersionMatcher {
    return CoreApi.getInstance().createAndVersionMatcher(left, right)
}

@OptIn(InternalApi::class)
@JvmName("OrVersionMatcher")
fun OrVersionMatcher(left: VersionMatcher, right: VersionMatcher): OrVersionMatcher {
    return CoreApi.getInstance().createOrVersionMatcher(left, right)
}

@OptIn(InternalApi::class)
@JvmName("createIncludeVersionMatcher")
fun IncludeVersionMatcher(value: Version): IncludeVersionMatcher {
    return CoreApi.getInstance().createIncludeVersionMatcher(value)
}

@OptIn(InternalApi::class)
@JvmName("createExcludeVersionMatcher")
fun ExcludeVersionMatcher(value: Version): ExcludeVersionMatcher {
    return CoreApi.getInstance().createExcludeVersionMatcher(value)
}

@OptIn(InternalApi::class)
@JvmName("createGreaterThanVersionMatcher")
fun GreaterThanVersionMatcher(version: Version): GreaterThanVersionMatcher {
    return CoreApi.getInstance().createGreaterThanVersionMatcher(version)
}

@OptIn(InternalApi::class)
@JvmName("createGreaterThanOrEqualVersionMatcher")
fun GreaterThanOrEqualVersionMatcher(version: Version): GreaterThanOrEqualVersionMatcher {
    return CoreApi.getInstance().createGreaterThanOrEqualVersionMatcher(version)
}

@OptIn(InternalApi::class)
@JvmName("createLessThanVersionMatcher")
fun LessThanVersionMatcher(version: Version): LessThanVersionMatcher {
    return CoreApi.getInstance().createLessThanVersionMatcher(version)
}

@OptIn(InternalApi::class)
@JvmName("createLessThanOrEqualVersionMatcher")
fun LessThanOrEqualVersionMatcher(version: Version): LessThanOrEqualVersionMatcher {
    return CoreApi.getInstance().createLessThanOrEqualVersionMatcher(version)
}

@OptIn(InternalApi::class)
@JvmName("createMajorVersionPrefixMatcher")
fun MajorVersionPrefixMatcher(major: Int): MajorVersionPrefixMatcher {
    return CoreApi.getInstance().createMajorVersionPrefixMatcher(major)
}

@OptIn(InternalApi::class)
@JvmName("createMajorMinorVersionPrefixMatcher")
fun MajorMinorVersionPrefixMatcher(major: Int, minor: Int): MajorMinorVersionPrefixMatcher {
    return CoreApi.getInstance().createMajorMinorVersionPrefixMatcher(major, minor)
}
