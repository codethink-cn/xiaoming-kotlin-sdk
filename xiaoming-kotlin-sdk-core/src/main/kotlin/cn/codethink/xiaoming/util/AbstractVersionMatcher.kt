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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = VersionMatcherSerializer::class)
@JsonDeserialize(using = VersionMatcherDeserializer::class)
abstract class AbstractVersionMatcher : VersionMatcher

object VersionMatcherSerializer : StdSerializer<VersionMatcher>(VersionMatcher::class.java) {
    private fun readResolve(): Any = VersionMatcherSerializer
    override fun serialize(value: VersionMatcher, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(value.toString())
    }
}

object VersionMatcherDeserializer : StdDeserializer<VersionMatcher>(VersionMatcher::class.java) {
    private fun readResolve(): Any = VersionMatcherDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): VersionMatcher {
        return parser.valueAsString.toVersionMatcher()
    }
}

class AndVersionMatcherImpl(
    override val left: VersionMatcher,
    override val right: VersionMatcher
) : AbstractVersionMatcher(), AndVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return left.isMatched(target) && right.isMatched(target)
    }

    override fun toString(): String = "($left & $right)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AndVersionMatcher

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

class OrVersionMatcherImpl(
    override val left: VersionMatcher,
    override val right: VersionMatcher
) : AbstractVersionMatcher(), OrVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return left.isMatched(target) || right.isMatched(target)
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
) : LiteralMatcher<Version>, IncludeVersionMatcher

@JvmInline
value class ExcludeVersionMatcherImpl(
    override val value: Version
) : ExcludeVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target != value
    }

    override fun toString(): String = "!$value"
}

@JvmInline
value class GreaterThanVersionMatcherImpl(
    override val version: Version
) : GreaterThanVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target > version
    }

    override fun toString(): String = ">$version"
}

@JvmInline
value class GreaterThanOrEqualVersionMatcherImpl(
    override val version: Version
) : GreaterThanOrEqualVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target >= version
    }

    override fun toString(): String = ">=$version"
}

@JvmInline
value class LessThanVersionMatcherImpl(
    override val version: Version
) : LessThanVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target < version
    }

    override fun toString(): String = "<$version"
}

@JvmInline
value class LessThanOrEqualVersionMatcherImpl(
    override val version: Version
) : LessThanOrEqualVersionMatcher {
    override fun isMatched(target: Version): Boolean {
        return target <= version
    }

    override fun toString(): String = "<=$version"
}

@JvmInline
value class MajorVersionPrefixMatcherImpl(
    override val major: Int
) : MajorVersionPrefixMatcher {
    override fun isMatched(target: Version): Boolean {
        return target.major == major
    }

    override fun toString(): String = "$major.+"
}

class MajorMinorVersionPrefixMatcherImpl(
    override val major: Int,
    override val minor: Int
) : MajorMinorVersionPrefixMatcher {
    override fun isMatched(target: Version): Boolean {
        return target.major == major && target.minor == minor
    }

    override fun toString(): String = "$major.$minor.+"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MajorMinorVersionPrefixMatcher

        if (major != other.major) return false
        if (minor != other.minor) return false

        return true
    }

    private val hashCodeCache = run {
        var result = major
        result = 31 * result + minor
        result
    }

    override fun hashCode(): Int = hashCodeCache
}