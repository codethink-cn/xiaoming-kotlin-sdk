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

@file:JvmName("Versions")

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Semantic version, see [Semantic Versioning 2.0.0](https://semver.org/).
 *
 * @author Chuanwise
 */
@JsonSerialize(using = VersionStringSerializer::class)
@JsonDeserialize(using = VersionStringDeserializer::class)
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val build: String? = null
) : Comparable<Version> {
    companion object {
        @JvmStatic
        @JavaFriendlyApi(replacement = "String.toVersion")
        fun parse(version: String): Version = version.toVersion()
    }

    private val toStringCache =
        "$major.$minor.$patch" + preRelease.prependOrNull("-").orEmpty() + build.prependOrNull("+").orEmpty()

    override fun toString(): String = toStringCache

    private val hashCodeCache = toStringCache.hashCode()
    override fun hashCode(): Int = hashCodeCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (patch != other.patch) return false
        if (preRelease != other.preRelease) return false
        if (build != other.build) return false
        if (toStringCache != other.toStringCache) return false
        if (hashCodeCache != other.hashCodeCache) return false

        return true
    }

    override fun compareTo(other: Version): Int {
        // Compare major, minor, and patch versions
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        // If versions are equal up to the patch, compare pre-release versions
        val preReleaseCompare = comparePreRelease(this.preRelease, other.preRelease)
        if (preReleaseCompare != 0) return preReleaseCompare

        // If pre-release versions are equal or one of them is null, compare build metadata
        // Versions without build metadata are considered higher than those with it
        return compareBuildMetadata(this.build, other.build)
    }

    private fun comparePreRelease(preRelease1: String?, preRelease2: String?): Int {
        if (preRelease1 == null && preRelease2 == null) return 0
        if (preRelease1 == null) return 1
        if (preRelease2 == null) return -1

        val parts1 = preRelease1.split('.')
        val parts2 = preRelease2.split('.')
        val minSize = minOf(parts1.size, parts2.size)

        for (i in 0 until minSize) {
            val part1 = parts1[i]
            val part2 = parts2[i]

            val part1Int = part1.toIntOrNull()
            val part2Int = part2.toIntOrNull()

            // If one is number, and the other is not, the number is higher.
            if (part1Int != part2Int) {
                return if (part1Int == null) 1 else -1
            }

            // If both are number, compare them as number.
            if (part1Int != null) {
                // If both parts are integers, compare them as integers.
                val compare = part1Int.compareTo(part2Int!!)
                if (compare != 0) return compare
            } else {
                // If one of the parts is not an integer, compare them as strings.
                val compare = part1.compareTo(part2)
                if (compare != 0) return compare
            }
        }

        // If all parts are equal, the version with fewer parts is higher.
        return parts1.size.compareTo(parts2.size)
    }

    private fun compareBuildMetadata(build1: String?, build2: String?): Int {
        if (build1 == null && build2 == null) return 0
        if (build1 == null) return -1
        if (build2 == null) return 1

        return build1.compareTo(build2)
    }
}

fun Version.toLiteralVersionMatcher() = IncludeVersionMatcher(this)

/**
 * Parse a version from a string, extract elements by index.
 * This regexp is from [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/).
 *
 * Element index: 0: major, 1: minor, 2: patch, 3: pre-release, 4: build
 */
val VERSION_STRING_REGEX: Regex = ("(0|[1-9]\\d*)\\" +
        ".(0|[1-9]\\d*)\\" +
        ".(0|[1-9]\\d*)" +
        "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
        "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?").toRegex()

/**
 * Parse a version from a string.
 *
 * @author Chuanwise
 */
fun String.toVersion(): Version = VERSION_STRING_REGEX.matchEntire(this)?.let { it ->
    val (major, minor, patch, preRelease, build) = it.destructured
    Version(
        major.toInt(), minor.toInt(), patch.toInt(),
        preRelease.takeIf { it.isNotEmpty() }, build.takeIf { it.isNotEmpty() }
    )
} ?: throw IllegalArgumentException(
    "Invalid version string: '$this', " +
            "make sure it matches the regex from the semantic versioning 2.0.0: $VERSION_STRING_REGEX."
)

object VersionStringSerializer : StdSerializer<Version>(Version::class.java) {
    private fun readResolve(): Any = VersionStringSerializer
    override fun serialize(version: Version, generator: JsonGenerator, serializerProvider: SerializerProvider) {
        generator.writeString(version.toString())
    }
}

object VersionStringDeserializer : StdDeserializer<Version>(Version::class.java) {
    private fun readResolve(): Any = VersionStringDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Version {
        return parser.valueAsString.toVersion()
    }
}
