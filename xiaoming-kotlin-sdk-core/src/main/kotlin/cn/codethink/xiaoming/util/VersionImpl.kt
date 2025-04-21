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

data class VersionImpl(
    override val major: Int,
    override val minor: Int,
    override val patch: Int,
    override val preRelease: String? = null,
    override val build: String? = null
) : Version {
    private val toStringCache = "$major.$minor.$patch" + preRelease.withPrefixOrNull("-").orEmpty() + build.withPrefixOrNull("+").orEmpty()

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
        return build == other.build
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
