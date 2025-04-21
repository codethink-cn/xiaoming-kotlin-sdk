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

abstract class AbstractVersionMatcher : VersionMatcher

class AndVersionMatcherImpl(
    override val left: VersionMatcher,
    override val right: VersionMatcher
) : AbstractVersionMatcher(), AndVersionMatcher {
    override fun matches(version: Version): Boolean {
        return left.matches(version) && right.matches(version)
    }

    override fun toString(): String = "($left & $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndVersionMatcher

        if (left != other.left) return false
        return right == other.right
    }

    private val hashCodeCache: Int = run {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        result
    }

    override fun hashCode(): Int = hashCodeCache
}

class OrVersionMatcherImpl(
    override val left: VersionMatcher,
    override val right: VersionMatcher
) : AbstractVersionMatcher(), OrVersionMatcher {
    override fun matches(target: Version): Boolean {
        return left.matches(target) || right.matches(target)
    }

    override fun toString(): String = "($left | $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrVersionMatcher

        if (left != other.left) return false
        if (right != other.right) return false

        return true
    }

    private val hashCodeCache: Int = run {
        var result = left.hashCode()
        result = 31 * result + right.hashCode()
        result
    }

    override fun hashCode(): Int = hashCodeCache
}

@JvmInline
value class IncludeVersionMatcherImpl(
    override val value: Version
) : IncludeVersionMatcher {
    override fun matches(version: Version): Boolean {
        return value == version
    }
}

@JvmInline
value class ExcludeVersionMatcherImpl(
    override val value: Version
) : ExcludeVersionMatcher {
    override fun matches(version: Version): Boolean {
        return version != value
    }

    override fun toString(): String = "!$value"
}

@JvmInline
value class GreaterThanVersionMatcherImpl(
    override val version: Version
) : GreaterThanVersionMatcher {
    override fun matches(target: Version): Boolean {
        return target > version
    }

    override fun toString(): String = ">$version"
}

@JvmInline
value class GreaterThanOrEqualVersionMatcherImpl(
    override val version: Version
) : GreaterThanOrEqualVersionMatcher {
    override fun matches(version: Version): Boolean {
        return version >= this.version
    }

    override fun toString(): String = ">=$version"
}

@JvmInline
value class LessThanVersionMatcherImpl(
    override val version: Version
) : LessThanVersionMatcher {
    override fun matches(version: Version): Boolean {
        return version < this.version
    }

    override fun toString(): String = "<$version"
}

@JvmInline
value class LessThanOrEqualVersionMatcherImpl(
    override val version: Version
) : LessThanOrEqualVersionMatcher {
    override fun matches(version: Version): Boolean {
        return version <= this.version
    }

    override fun toString(): String = "<=$version"
}

@JvmInline
value class MajorVersionPrefixMatcherImpl(
    override val major: Int
) : MajorVersionPrefixMatcher {
    override fun matches(version: Version): Boolean {
        return version.major == major
    }

    override fun toString(): String = "$major.+"
}

data class MajorMinorVersionPrefixMatcherImpl(
    override val major: Int,
    override val minor: Int
) : MajorMinorVersionPrefixMatcher {
    override fun matches(version: Version): Boolean {
        return version.major == major && version.minor == minor
    }

    override fun toString(): String = "$major.$minor.+"
}