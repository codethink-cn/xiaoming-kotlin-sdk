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

package cn.codethink.xiaoming.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

const val SEGMENT_SEPARATOR = "."
val SEGMENT_REGEX = "[\\w-]+".toRegex()
val SEGMENT_ID_REGEX = "[\\w-]+(\\.[\\w-]+)*".toRegex()

/**
 * A segment id is a string that contains several segments (not empty),
 * separated by a dot. Each segment should match the [SEGMENT_REGEX].
 *
 * For example, `a.b.c` is a valid segment id, while `a..b` is not.
 *
 * @author Chuanwise
 * @see DefaultStringListMatcher
 */
@JsonSerialize(using = SegmentIdStringSerializer::class)
@JsonDeserialize(using = SegmentIdStringDeserializer::class)
data class SegmentId(
    private val segments: List<String>
) : Id, List<String> by segments {
    init {
        assert(segments.isNotEmpty()) { "Segments should not be empty." }
        assert(segments.all { it.matches(SEGMENT_REGEX) }) { "Segments should match the regexp: $SEGMENT_REGEX." }
    }

    companion object {
        @JvmStatic
        @JavaFriendlyApi
        fun parse(string: String): SegmentId = segmentIdOf(string)
    }

    fun toList(): List<String> = segments

    private val toStringCache: String = segments.joinToString(SEGMENT_SEPARATOR)
    override fun toString(): String = toStringCache

    override fun hashCode(): Int = toStringCache.hashCode()
    override fun equals(other: Any?): Boolean = other is SegmentId && other.toString() == toString()
}

fun segmentIdOf(string: String): SegmentId = SegmentId(string.split("."))
fun String.toSegmentId(): SegmentId = segmentIdOf(this)

object SegmentIdStringSerializer : StdSerializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringSerializer
    override fun serialize(segmentId: SegmentId, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(segmentId.toString())
    }
}

object SegmentIdStringDeserializer : StdDeserializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringDeserializer
    override fun deserialize(
        parser: com.fasterxml.jackson.core.JsonParser,
        context: com.fasterxml.jackson.databind.DeserializationContext
    ): SegmentId {
        return segmentIdOf(parser.text)
    }
}

data class DefaultStringListMatchingContext(
    val matcher: DefaultStringListMatcher,
    val target: List<String>,
    val matchers: List<Matcher<String>>,
    var matcherIndex: Int = 0,
    var targetIndex: Int = 0,
    var result: Boolean? = null
)

/**
 * If matcher implements this interface, it will be called when matching. Or [Matcher.isMatched] instead.
 *
 * Notice that implementations MUST maintain the index
 * [DefaultStringListMatchingContext.matcherIndex] and [DefaultStringListMatchingContext.targetIndex].
 *
 * @author Chuanwise
 */
interface DefaultStringListMatcherMatchingCallbackSupport : Matcher<String> {
    fun onDefaultStringListMatcherMatching(context: DefaultStringListMatchingContext): Boolean
}

data class DefaultStringListMatcherConstructingContext(
    val matcher: DefaultStringListMatcher,
    val matchers: List<Matcher<String>>,
    var matcherIndex: Int
)

/**
 * If matcher implements this interface, it will be called when constructing to check matchers.
 *
 * Notice that implementation DON'T HAVE TO maintain the index
 * [DefaultStringListMatcherConstructingContext.matcherIndex]. If it's not changed, it will increase
 * automatically.
 *
 * @author Chuanwise
 */
interface DefaultStringListMatcherConstructingCallbackSupport : Matcher<String> {
    fun onDefaultStringListMatcherConstructing(context: DefaultStringListMatcherConstructingContext)
}

/**
 * Use some [matchers] to match a list of strings.
 *
 * Notice that the [matchers] must match the rule defined in the implementation classes
 * of [DefaultStringListMatcherMatchingCallbackSupport] and
 * [DefaultStringListMatcherConstructingCallbackSupport] in [matchers]).
 *
 * @author Chuanwise
 * @see AnyMatcher
 * @see WildcardStringMatcher
 */
class DefaultStringListMatcher(
    val matchers: List<Matcher<String>>
) : Matcher<List<String>> {
    init {
        if (matchers.isEmpty()) {
            throw IllegalArgumentException("Segment matchers should not be empty.")
        }

        val context = DefaultStringListMatcherConstructingContext(this, matchers, 0)
        while (context.matcherIndex < matchers.size) {
            if (matchers is DefaultStringListMatcherConstructingCallbackSupport) {
                // Text matcher can change the index. If not change, the index will increase automatically.
                val index = context.matcherIndex
                matchers.onDefaultStringListMatcherConstructing(context)
                if (index == context.matcherIndex) {
                    context.matcherIndex++
                }
            } else {
                context.matcherIndex++
            }
        }
    }

    override val type: String = SEGMENT_ID_MATCHER_TYPE_DEFAULT

    @Suppress("UNCHECKED_CAST")
    override val targetType: Class<List<String>>
        get() = List::class.java as Class<List<String>>

    override fun isMatched(target: List<String>): Boolean {
        val context = DefaultStringListMatchingContext(this, target, matchers)
        while (context.matcherIndex < matchers.size && context.targetIndex < target.size) {
            when (val matcher = matchers[context.matcherIndex]) {
                is DefaultStringListMatcherMatchingCallbackSupport -> {
                    if (!matcher.onDefaultStringListMatcherMatching(context)) {
                        return false
                    }
                    if (context.result != null) {
                        return context.result!!
                    }
                }

                else -> {
                    if (!matcher.isMatched(target[context.targetIndex])) {
                        return false
                    }
                    context.matcherIndex++
                    context.targetIndex++
                }
            }
        }
        return context.matcherIndex == matchers.size && context.targetIndex == target.size
    }

    private val toStringCache: String by lazy {
        "DefaultStringListMatcher(" +
                "type=$SEGMENT_ID_MATCHER_TYPE_DEFAULT," +
                "segmentMatchers=$matchers)"
    }

    override fun toString(): String = toStringCache
}
