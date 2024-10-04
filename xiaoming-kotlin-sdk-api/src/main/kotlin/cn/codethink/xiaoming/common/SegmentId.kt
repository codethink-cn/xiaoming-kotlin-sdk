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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.apache.commons.text.StringEscapeUtils

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
 * @see DefaultSegmentIdMatcher
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
fun String.toSegmentIdOrNull(): SegmentId? {
    return try {
        toSegmentId()
    } catch (_: Throwable) {
        null
    }
}
fun List<String>.toSegmentId(): SegmentId = SegmentId(this)

object SegmentIdStringSerializer : StdSerializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringSerializer
    override fun serialize(segmentId: SegmentId, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeString(segmentId.toString())
    }
}

object SegmentIdStringDeserializer : StdDeserializer<SegmentId>(SegmentId::class.java) {
    private fun readResolve(): Any = SegmentIdStringDeserializer
    override fun deserialize(parser: JsonParser, context: DeserializationContext): SegmentId {
        return segmentIdOf(parser.valueAsString)
    }
}

data class DefaultStringListMatchingContext(
    val matcher: DefaultSegmentIdMatcher,
    val target: SegmentId,
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
    fun onDefaultStringListMatcherMatchingRemaining(context: DefaultStringListMatchingContext): Boolean = false
    fun onDefaultStringListMatcherMatchingEmpty(context: DefaultStringListMatchingContext): Boolean = false
}

data class DefaultStringListMatcherConstructingContext(
    val matcher: DefaultSegmentIdMatcher,
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
 * Match any amount of texts.
 *
 * Notice that:
 *
 * 1. It can not appear continuously.
 * 2. The next matcher can not be [WildcardStringMatcher] or [AnyMatcher].
 *
 * If [optional] is true, the matched texts can be empty. If [policy] is true,
 *
 * @author Chuanwise
 */
@JsonTypeName(STRING_MATCHER_TYPE_WILDCARD)
class WildcardStringMatcher private constructor(
    val majority: Boolean,
    val optional: Boolean,
    val count: Int? = null
) : Matcher<String>, DefaultStringListMatcherConstructingCallbackSupport,
    DefaultStringListMatcherMatchingCallbackSupport {
    companion object {
        @JvmStatic
        val MAJORITY_OPTIONAL = WildcardStringMatcher(majority = true, optional = true)

        @JvmStatic
        val MINORITY_OPTIONAL = WildcardStringMatcher(majority = false, optional = true)

        @JvmStatic
        val MAJORITY_REQUIRED = WildcardStringMatcher(majority = true, optional = false)

        @JvmStatic
        val MINORITY_REQUIRED = WildcardStringMatcher(majority = false, optional = false)

        @JvmStatic
        val MINORITY_OPTIONAL_ONCE = WildcardStringMatcher(majority = false, optional = true, count = 1)

        @JvmStatic
        val MINORITY_REQUIRED_ONCE = WildcardStringMatcher(majority = false, optional = false, count = 1)

        @JvmStatic
        @JsonCreator
        fun of(majority: Boolean, optional: Boolean, count: Int? = null): WildcardStringMatcher = when (count) {
            null -> when (majority) {
                true -> if (optional) MAJORITY_OPTIONAL else MAJORITY_REQUIRED
                false -> if (optional) MINORITY_OPTIONAL else MINORITY_REQUIRED
            }

            1 -> when (majority) {
                true -> throw IllegalArgumentException("Majority can not be once.")
                false -> if (optional) MINORITY_OPTIONAL_ONCE else MINORITY_REQUIRED_ONCE
            }

            else -> WildcardStringMatcher(majority, optional, count)
        }
    }

    init {
        count?.let {
            if (it <= 0 || majority) {
                throw IllegalArgumentException("If count provided, it should be positive and minority.")
            }
        }
    }

    override val type: String = STRING_MATCHER_TYPE_WILDCARD

    override val targetType: Class<String> = String::class.java

    override val targetNullable: Boolean = false

    override fun isMatched(target: String): Boolean = true

    override fun onDefaultStringListMatcherConstructing(context: DefaultStringListMatcherConstructingContext) {
        if (context.matcherIndex > 0) {
            // If it is not the first one, check if previous one is `WildcardStringMatcher`.
            val previous = context.matchers[context.matcherIndex - 1]
            if (previous == AnyMatcher<String>()) {
                throw IllegalArgumentException(
                    "AnyMatcher<String> next to WildcardStringMatcher near index ${context.matcherIndex}!"
                )
            }
        }
        if (context.matcherIndex < context.matchers.size - 1) {
            // If it is not the last one, check if next one is `WildcardStringMatcher` or any optional matcher.
            val next = context.matchers[context.matcherIndex + 1]
            if (next is WildcardStringMatcher || next !== AnyMatcher<String>()) {
                throw IllegalArgumentException(
                    "WildcardStringMatcher appear continuously near index ${context.matcherIndex}!"
                )
            }
        }
    }

    override fun onDefaultStringListMatcherMatching(context: DefaultStringListMatchingContext): Boolean {
        // If current matcher is the last one, and the element is the last one, matched.
        if (context.matcherIndex == context.matchers.size - 1) {
            if (context.targetIndex == context.target.size - 1) {
                context.result = true
                return true
            } else {
                context.result = count == null
                context.targetIndex++
                return count == null
            }
        }

        context.matcherIndex++
        val nextMatcher = context.matchers[context.matcherIndex]

        // The matcher after WildcardStringMatcher must be functional.
        if (nextMatcher is WildcardStringMatcher ||
            nextMatcher == AnyMatcher<String>()
        ) {

            throw IllegalArgumentException(
                "WildcardStringMatcher can not followed " +
                        "by another WildcardStringMatcher or AnyMatcher<String>()."
            )
        }

        // Or get the next matcher and find matched segment.
        var nextTargetIndex: Int
        if (majority) {
            // Majority.
            nextTargetIndex = context.target.size - 1
            while (nextTargetIndex >= context.targetIndex) {
                val nextSegment = context.target[nextTargetIndex]
                if (nextMatcher.isMatched(nextSegment)) {
                    break
                }
                nextTargetIndex--
            }
            if ((nextTargetIndex < context.targetIndex && optional) ||
                (nextTargetIndex <= context.targetIndex && !optional)
            ) {
                context.result = false
                return false
            }
            context.targetIndex = nextTargetIndex + 1
            context.matcherIndex++
        } else {
            // Minority.
            nextTargetIndex = context.targetIndex
            var count = count ?: 0
            while (nextTargetIndex < context.target.size) {
                val nextSegment = context.target[nextTargetIndex]
                if (nextMatcher.isMatched(nextSegment)) {
                    count--
                    if (count < 0) {
                        break
                    }
                }
                nextTargetIndex++
            }
            if (nextTargetIndex == context.targetIndex && !optional) {
                context.result = false
                return false
            }
            context.targetIndex = nextTargetIndex
        }

        return true
    }

    override fun onDefaultStringListMatcherMatchingRemaining(context: DefaultStringListMatchingContext): Boolean {
        return optional
    }

    override fun onDefaultStringListMatcherMatchingEmpty(context: DefaultStringListMatchingContext): Boolean {
        return optional
    }

    private val toStringCache: String by lazy {
        "WildcardStringMatcher(" +
                "majority=$majority," +
                "optional=$optional," +
                "count=$count)"
    }

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WildcardStringMatcher) return false

        if (majority != other.majority) return false
        if (optional != other.optional) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = majority.hashCode()
        result = 31 * result + optional.hashCode()
        result = 31 * result + (count ?: 0)
        return result
    }
}

val MajorityOptionalWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MAJORITY_OPTIONAL

val MajorityRequiredWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MAJORITY_REQUIRED

val MinorityOptionalWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_OPTIONAL

val MinorityRequiredWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_REQUIRED

val MinorityOptionalOnceWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_OPTIONAL_ONCE

val MinorityRequiredOnceWildcardStringMatcher: WildcardStringMatcher
    get() = WildcardStringMatcher.MINORITY_REQUIRED_ONCE


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
@JsonTypeName(SEGMENT_ID_MATCHER_TYPE_DEFAULT)
class DefaultSegmentIdMatcher(
    val matchers: List<Matcher<String>>
) : Matcher<SegmentId> {
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

    override val targetType: Class<SegmentId> = SegmentId::class.java

    override val targetNullable: Boolean = false


    override fun isMatched(target: SegmentId): Boolean {
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

        // If matcher is not finished, but target is finished.
        // Test if remaining is optional.
        while (context.matcherIndex < matchers.size) {
            val matcherIndex = context.matcherIndex
            val matcher = matchers[context.matcherIndex]
            if (matcher is DefaultStringListMatcherMatchingCallbackSupport) {
                if (!matcher.onDefaultStringListMatcherMatchingRemaining(context)) {
                    return false
                }
                if (context.result != null) {
                    return context.result!!
                }
                if (context.matcherIndex == matcherIndex) {
                    context.matcherIndex++
                }
            } else {
                return false
            }
        }

        // If target is not finished, but matcher is finished.
        // Try to use the last matcher to match the all.
        if (context.targetIndex < target.size) {
            val lastMatcher = matchers[context.matcherIndex - 1]
            if (lastMatcher is DefaultStringListMatcherMatchingCallbackSupport) {
                while (context.targetIndex < target.size) {
                    val targetIndex = context.targetIndex

                    if (!lastMatcher.onDefaultStringListMatcherMatchingRemaining(context)) {
                        return false
                    }
                    if (context.result != null) {
                        return context.result!!
                    }

                    if (targetIndex == context.targetIndex) {
                        context.targetIndex++
                    }
                }
                return true
            } else {
                return false
            }
        }

        return true
    }

    private val toStringCache: String by lazy {
        "DefaultStringListMatcher(" +
                "type=$SEGMENT_ID_MATCHER_TYPE_DEFAULT," +
                "segmentMatchers=$matchers)"
    }

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultSegmentIdMatcher

        return matchers == other.matchers
    }

    override fun hashCode(): Int {
        return matchers.hashCode()
    }
}

val MINORITY_REQUIRED_WILDCARD_STRING_MATCHER_REGEX = "(\\d+)?\\+{2}".toRegex()
val MINORITY_OPTIONAL_WILDCARD_STRING_MATCHER_REGEX = "(\\d+)?\\?{2}".toRegex()

fun String.toStringMatcher(): Matcher<String> {
    if (isEmpty()) {
        throw IllegalArgumentException("String matcher should not be empty.")
    }

    MINORITY_REQUIRED_WILDCARD_STRING_MATCHER_REGEX.matchEntire(this)?.let {
        val count = it.groupValues[1].toIntOrNull()
        return WildcardStringMatcher.of(majority = false, optional = false, count = count)
    }
    MINORITY_OPTIONAL_WILDCARD_STRING_MATCHER_REGEX.matchEntire(this)?.let {
        val count = it.groupValues[1].toIntOrNull()
        return WildcardStringMatcher.of(majority = false, optional = true, count = count)
    }

    when (this) {
        "+" -> return WildcardStringMatcher.MINORITY_REQUIRED_ONCE
        "?" -> return WildcardStringMatcher.MINORITY_OPTIONAL_ONCE
        "+++" -> return WildcardStringMatcher.MAJORITY_REQUIRED
        "???", "*" -> return WildcardStringMatcher.MAJORITY_OPTIONAL
        else -> {
            if (startsWith("{") && endsWith("}")) {
                val pattern = substring(1, length - 1)
                if (pattern.isEmpty()) {
                    throw IllegalArgumentException("Empty regex string matcher.")
                }
                return RegexStringMatcher(Regex(pattern))
            }
            if (startsWith("\"") && endsWith("\"")) {
                val unescaped = StringEscapeUtils.unescapeJson(substring(1, length - 1))
                if (unescaped.isEmpty()) {
                    throw IllegalArgumentException("Empty literal string matcher.")
                }
                return LiteralStringMatcher(unescaped)
            }
            return LiteralStringMatcher(this)
        }
    }
}

/**
 * Compile expression of [DefaultSegmentIdMatcher] to its object.
 *
 * BNF pattern:
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
 * @see AnyMatcher
 */
fun compileSegmentIdMatcher(string: String): Matcher<SegmentId> {
    if (string.isEmpty()) {
        throw IllegalArgumentException("Segment matcher should not be empty.")
    }

    val matchers = mutableListOf<Matcher<String>>()
    val stringBuilder = StringBuilder()

    val acceptNewMatcherState = 0
    val acceptingMatcherState = 1

    var escapeSupport = false
    var escaping = false

    var state = acceptNewMatcherState
    var index = 0

    while (index < string.length) {
        when (state) {
            acceptNewMatcherState -> {
                when (val char = string[index]) {
                    '"' -> {
                        escapeSupport = true
                        state = acceptingMatcherState
                    }

                    '.' -> {
                        throw IllegalArgumentException("Empty matcher at index $index.")
                    }

                    '{' -> {
                        escapeSupport = true
                        state = acceptingMatcherState
                        stringBuilder.append(char)
                    }

                    else -> {
                        escapeSupport = false
                        state = acceptingMatcherState
                        stringBuilder.append(char)
                    }
                }
                index++
            }

            acceptingMatcherState -> {
                when (val char = string[index]) {
                    '\\' -> {
                        if (escapeSupport) {
                            if (escaping) {
                                stringBuilder.append('\\')
                                escaping = false
                            } else {
                                escaping = true
                            }
                        } else {
                            stringBuilder.append('\\')
                        }
                    }

                    '.' -> {
                        if (escapeSupport) {
                            if (escaping) {
                                stringBuilder.append('.')
                                escaping = false
                            }
                        }
                        matchers.add(stringBuilder.toString().toStringMatcher())
                        stringBuilder.clear()
                        state = acceptNewMatcherState
                    }

                    else -> {
                        stringBuilder.append(char)
                        state = acceptingMatcherState
                    }
                }
                index++
            }
        }
    }
    when (state) {
        acceptNewMatcherState -> {
            throw IllegalArgumentException("Empty matcher at the end.")
        }

        acceptingMatcherState -> {
            matchers.add(stringBuilder.toString().toStringMatcher())
        }
    }

    return DefaultSegmentIdMatcher(matchers)
}

@JsonTypeName(SEGMENT_ID_MATCHER_TYPE_LITERAL)
class LiteralSegmentIdMatcher(
    val id: SegmentId
) : Matcher<SegmentId> {
    override val type: String = SEGMENT_ID_MATCHER_TYPE_LITERAL

    override val targetType: Class<SegmentId> = SegmentId::class.java

    override val targetNullable: Boolean = false

    override fun isMatched(target: SegmentId): Boolean = target == id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteralSegmentIdMatcher

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    private val toStringCache: String by lazy {
        "LiteralStringListMatcher(" +
                "type=$SEGMENT_ID_MATCHER_TYPE_LITERAL," +
                "list=$id)"
    }

    override fun toString(): String = toStringCache
}

fun SegmentId.toLiteralMatcher() = LiteralSegmentIdMatcher(this)
