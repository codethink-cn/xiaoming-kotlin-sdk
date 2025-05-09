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

abstract class AbstractVersionPattern : VersionPattern

class AndVersionPatternImpl(
    override val left: VersionPattern,
    override val right: VersionPattern
) : AbstractVersionPattern(), AndVersionPattern {
    override fun matches(version: Version): Boolean {
        return left.matches(version) && right.matches(version)
    }

    override fun toString(): String = "($left & $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndVersionPattern

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

class OrVersionPatternImpl(
    override val left: VersionPattern,
    override val right: VersionPattern
) : AbstractVersionPattern(), OrVersionPattern {
    override fun matches(target: Version): Boolean {
        return left.matches(target) || right.matches(target)
    }

    override fun toString(): String = "($left | $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrVersionPattern

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
value class IncludeVersionPatternImpl(
    override val value: Version
) : IncludeVersionPattern {
    override fun matches(version: Version): Boolean {
        return value == version
    }
}

@JvmInline
value class ExcludeVersionPatternImpl(
    override val value: Version
) : ExcludeVersionPattern {
    override fun matches(version: Version): Boolean {
        return version != value
    }

    override fun toString(): String = "!$value"
}

@JvmInline
value class GreaterThanVersionPatternImpl(
    override val version: Version
) : GreaterThanVersionPattern {
    override fun matches(target: Version): Boolean {
        return target > version
    }

    override fun toString(): String = ">$version"
}

@JvmInline
value class GreaterThanOrEqualVersionPatternImpl(
    override val version: Version
) : GreaterThanOrEqualVersionPattern {
    override fun matches(version: Version): Boolean {
        return version >= this.version
    }

    override fun toString(): String = ">=$version"
}

@JvmInline
value class LessThanVersionPatternImpl(
    override val version: Version
) : LessThanVersionPattern {
    override fun matches(version: Version): Boolean {
        return version < this.version
    }

    override fun toString(): String = "<$version"
}

@JvmInline
value class LessThanOrEqualVersionPatternImpl(
    override val version: Version
) : LessThanOrEqualVersionPattern {
    override fun matches(version: Version): Boolean {
        return version <= this.version
    }

    override fun toString(): String = "<=$version"
}

@JvmInline
value class MajorVersionPrefixPatternImpl(
    override val major: Int
) : MajorVersionPrefixPattern {
    override fun matches(version: Version): Boolean {
        return version.major == major
    }

    override fun toString(): String = "$major.+"
}

data class MajorMinorVersionPrefixPatternImpl(
    override val major: Int,
    override val minor: Int
) : MajorMinorVersionPrefixPattern {
    override fun matches(version: Version): Boolean {
        return version.major == major && version.minor == minor
    }

    override fun toString(): String = "$major.$minor.+"
}