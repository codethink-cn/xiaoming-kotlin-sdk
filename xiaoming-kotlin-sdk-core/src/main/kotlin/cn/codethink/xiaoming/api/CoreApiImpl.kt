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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.permission.Permission
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.permission.PluginRequirementImpl
import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.util.AndVersionMatcher
import cn.codethink.xiaoming.util.AndVersionMatcherImpl
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.CauseImpl
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.DataImpl
import cn.codethink.xiaoming.util.ExcludeVersionMatcher
import cn.codethink.xiaoming.util.ExcludeVersionMatcherImpl
import cn.codethink.xiaoming.util.GreaterThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanOrEqualVersionMatcherImpl
import cn.codethink.xiaoming.util.GreaterThanVersionMatcher
import cn.codethink.xiaoming.util.GreaterThanVersionMatcherImpl
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.IncludeVersionMatcher
import cn.codethink.xiaoming.util.IncludeVersionMatcherImpl
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcherImpl
import cn.codethink.xiaoming.util.LessThanVersionMatcher
import cn.codethink.xiaoming.util.LessThanVersionMatcherImpl
import cn.codethink.xiaoming.util.ListSegmentIdMatcherImpl
import cn.codethink.xiaoming.util.LiteralPermissionMatcher
import cn.codethink.xiaoming.util.LiteralSegmentIdMatcherImpl
import cn.codethink.xiaoming.util.LiteralStringMatcherImpl
import cn.codethink.xiaoming.util.LongIdImpl
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcherImpl
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcherImpl
import cn.codethink.xiaoming.util.MajorityOptionalWildcardStringMatcher
import cn.codethink.xiaoming.util.MajorityRequiredWildcardStringMatcher
import cn.codethink.xiaoming.util.Matcher
import cn.codethink.xiaoming.util.MinorityOptionalOnceWildcardStringMatcher
import cn.codethink.xiaoming.util.MinorityRequiredOnceWildcardStringMatcher
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NamespaceIdImpl
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.OrVersionMatcher
import cn.codethink.xiaoming.util.OrVersionMatcherImpl
import cn.codethink.xiaoming.util.PluginSubjectDescriptorMatcher
import cn.codethink.xiaoming.util.PluginSubjectDescriptorMatcherImpl
import cn.codethink.xiaoming.util.Raw
import cn.codethink.xiaoming.util.RegexStringMatcherImpl
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SegmentIdImpl
import cn.codethink.xiaoming.util.SegmentIdMatcher
import cn.codethink.xiaoming.util.StandardCause
import cn.codethink.xiaoming.util.StandardCauseImpl
import cn.codethink.xiaoming.util.StringId
import cn.codethink.xiaoming.util.StringIdImpl
import cn.codethink.xiaoming.util.StringMatcher
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.TemplateImpl
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import cn.codethink.xiaoming.util.TestSubjectDescriptorImpl
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.UniversalUniqueId
import cn.codethink.xiaoming.util.UniversalUniqueIdImpl
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionImpl
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.WildcardStringMatcher
import cn.codethink.xiaoming.util.WildcardStringMatcherImpl
import cn.codethink.xiaoming.util.toMillisecondsTime
import cn.codethink.xiaoming.util.toNamespaceId
import cn.codethink.xiaoming.util.toSegmentId
import cn.codethink.xiaoming.util.toSingleSegmentId
import cn.codethink.xiaoming.util.toStringMatcher
import cn.codethink.xiaoming.util.toVersion
import cn.codethink.xiaoming.util.toVersionMatcher
import org.apache.commons.text.StringEscapeUtils
import java.util.UUID

@InternalApi
class CoreApiImpl : CoreApi {
    // Id
    override fun parseId(string: String): Id {
        return if (':' in string) {
            parseNamespaceId(string)
        } else if ('.' in string) {
            parseSegmentId(string)
        } else {
            createStringId(string)
        }
    }

    override fun toStringId(id: Id): StringId {
        return when (id) {
            is StringId -> id
            else -> createStringId(id.toString())
        }
    }

    override fun toNumericalId(id: Id): NumericalId {
        return when (id) {
            is NumericalId -> id
            else -> createNumericalId(id.toString().toLong())
        }
    }

    override fun toNamespaceId(id: Id): NamespaceId {
        return when (id) {
            is NamespaceId -> id
            else -> toString().toNamespaceId()
        }
    }

    override fun toSegmentId(id: Id): SegmentId {
        return when (id) {
            is SegmentId -> id
            else -> id.toString().toSegmentId()
        }
    }

    override fun createSegmentId(segments: List<String>): SegmentId {
        return SegmentIdImpl(segments)
    }

    override fun parseSegmentId(string: String): SegmentId {
        return SegmentIdImpl(string.split("."))
    }

    override fun createNumericalId(value: Long): NumericalId {
        return LongIdImpl(value)
    }

    override fun createNumericalId(value: Int): NumericalId {
        return LongIdImpl(value.toLong())
    }

    private val emptyStringId = StringIdImpl("")

    override fun createStringId(string: String): StringId {
        return if (string.isEmpty()) {
            emptyStringId
        } else {
            StringIdImpl(string)
        }
    }

    override fun createNamespaceId(group: SegmentId, name: String): NamespaceId {
        return NamespaceIdImpl(group, name.toSingleSegmentId())
    }

    override fun createNamespaceId(group: SegmentId, name: SegmentId): NamespaceId {
        return NamespaceIdImpl(group, name)
    }

    override fun parseNamespaceId(string: String): NamespaceId {
        val split = string.split(':')
        require(split.size == 2) {
            "Namespace id string should contain exactly one colon."
        }
        return NamespaceIdImpl(split[0].toSegmentId(), split[1].toSegmentId())
    }

    override fun createRandomUniversalUniqueId(): UniversalUniqueId {
        return UniversalUniqueIdImpl(UUID.randomUUID())
    }

    override fun createUniversalUniqueId(uuid: UUID): UniversalUniqueId {
        return UniversalUniqueIdImpl(uuid)
    }

    // Template
    override fun parseTemplate(format: String): Template {
        return TemplateImpl(format)
    }

    // Cause
    override fun createCause(message: String, subject: SubjectDescriptor): Cause {
        return CauseImpl(message, subject)
    }

    override fun createEmptyTextCause(
        id: Id,
        text: String,
        subject: SubjectDescriptor,
        cause: Cause?
    ): StandardCause {
        return StandardCauseImpl(id, text, subject, cause)
    }

    // Subject Descriptor
    override fun createTestSubjectDescriptor(): TestSubjectDescriptor = TestSubjectDescriptorImpl

    // Data
    override fun createData(raw: Raw): Data {
        return DataImpl(raw)
    }

    // Time
    override fun createTimeOfMilliseconds(milliseconds: Long): Time = milliseconds.toMillisecondsTime()

    // PermissionMatchers
    override fun createLiteralPermissionMatcher(permission: Permission): PermissionMatcher {
        return LiteralPermissionMatcher(permission)
    }

    // PluginMetaMatcher
    override fun parsePluginRequirement(string: String): PluginRequirement {
        require(string.isNotEmpty()) {
            "Plugin requirement string should not be empty."
        }

        var length = string.length

        val optional: Boolean
        val local: Boolean

        if (string.endsWith("?!") || string.endsWith("!?")) {
            local = true
            optional = true

            length -= 2
        } else if (string.endsWith("!")) {
            local = true
            optional = false

            length -= 1
        } else if (string.endsWith("?")) {
            local = false
            optional = true

            length -= 1
        } else {
            local = false
            optional = false
        }

        val colonIndexAfterGroup = string.indexOf(':', 0)
        require(colonIndexAfterGroup != -1) {
            "Plugin dependency string should contain a colon."
        }
        val group = string.substring(0, colonIndexAfterGroup).toSegmentId()

        val colonIndexAfterName = string.indexOf(':', colonIndexAfterGroup + 1)
        val name = if (colonIndexAfterName == -1) {
            string.substring(colonIndexAfterGroup + 1)
        } else {
            string.substring(colonIndexAfterGroup + 1, colonIndexAfterName)
        }

        val pluginId = createNamespaceId(group, name)

        val atIndex = string.lastIndexOf('@', length)
        val channel = if (atIndex == -1) {
            null
        } else {
            string.substring(atIndex + 1, length).toStringMatcher()
        }

        val atIndexOrLength = if (atIndex == -1) length else atIndex
        val version = if (atIndexOrLength == colonIndexAfterName + 1) null else {
            string.substring(colonIndexAfterName + 1, atIndexOrLength).toVersionMatcher()
        }

        return PluginRequirementImpl(pluginId, version, channel, optional, local)
    }

    override fun createPluginRequirement(
        id: NamespaceId,
        version: VersionMatcher?,
        channel: StringMatcher?,
        optional: Boolean,
        local: Boolean
    ): PluginRequirement {
        return PluginRequirementImpl(id, version, channel, optional, local)
    }

    // StringMatcher
    companion object {
        private val MINORITY_REQUIRED_WILDCARD_STRING_MATCHER_REGEX = "(\\d+)?\\+{2}".toRegex()
        private val MINORITY_OPTIONAL_WILDCARD_STRING_MATCHER_REGEX = "(\\d+)?\\?{2}".toRegex()
    }

    override fun parseStringMatcher(string: String): StringMatcher {
        if (string.isEmpty()) {
            throw IllegalArgumentException("String matcher should not be empty.")
        }

        MINORITY_REQUIRED_WILDCARD_STRING_MATCHER_REGEX.matchEntire(string)?.let {
            val count = it.groupValues[1].toIntOrNull()
            return cn.codethink.xiaoming.util.WildcardStringMatcher.Companion.of(
                majority = false,
                optional = false,
                count = count
            )
        }
        MINORITY_OPTIONAL_WILDCARD_STRING_MATCHER_REGEX.matchEntire(string)?.let {
            val count = it.groupValues[1].toIntOrNull()
            return cn.codethink.xiaoming.util.WildcardStringMatcher.Companion.of(
                majority = false,
                optional = true,
                count = count
            )
        }

        when (string) {
            "+" -> return MinorityRequiredOnceWildcardStringMatcher
            "?" -> return MinorityOptionalOnceWildcardStringMatcher
            "+++" -> return MajorityRequiredWildcardStringMatcher
            "???", "*" -> return MajorityOptionalWildcardStringMatcher
            else -> {
                if (string.startsWith("{") && string.endsWith("}")) {
                    val pattern = string.substring(1, string.length - 1)
                    if (pattern.isEmpty()) {
                        throw IllegalArgumentException("Empty regex string matcher.")
                    }
                    return RegexStringMatcherImpl(Regex(pattern))
                }
                if (string.startsWith("\"") && string.endsWith("\"")) {
                    val unescaped = StringEscapeUtils.unescapeJson(string.substring(1, string.length - 1))
                    if (unescaped.isEmpty()) {
                        throw IllegalArgumentException("Empty literal string matcher.")
                    }
                    return LiteralStringMatcherImpl(unescaped)
                }
                return LiteralStringMatcherImpl(string)
            }
        }
    }

    override fun createLiteralStringMatcher(string: String): StringMatcher {
        return LiteralStringMatcherImpl(string)
    }

    override fun createRegexStringMatcher(regex: String): StringMatcher {
        return RegexStringMatcherImpl(Regex(regex))
    }

    override fun createWildcardStringMatcher(majority: Boolean, optional: Boolean, count: Int?): WildcardStringMatcher {
        return WildcardStringMatcherImpl.of(majority, optional, count)
    }

    // SegmentIdMatcher
    override fun parseSegmentIdMatcher(string: String): SegmentIdMatcher {
        if (string.isEmpty()) {
            throw IllegalArgumentException("Segment matcher should not be empty.")
        }

        val matchers = mutableListOf<StringMatcher>()
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

        return ListSegmentIdMatcherImpl(matchers)
    }

    override fun createSegmentIdMatcher(matchers: List<StringMatcher>): SegmentIdMatcher {
        return ListSegmentIdMatcherImpl(matchers)
    }

    override fun createSegmentIdMatcher(segmentId: SegmentId): SegmentIdMatcher {
        return LiteralSegmentIdMatcherImpl(segmentId)
    }

    // PluginSubjectDescriptorMatcher
    override fun createPluginSubjectDescriptorMatcher(id: Matcher<NamespaceId>): PluginSubjectDescriptorMatcher {
        return PluginSubjectDescriptorMatcherImpl(id)
    }

    // VersionMatcher
    override fun parseVersionMatcher(string: String): VersionMatcher {
        string.apply {
            require(isNotEmpty()) { "Version matcher string must not be empty." }

            // 1. Tokenize.
            abstract class Token
            class OperatorToken(val operator: String) : Token()
            class VersionMatcherToken(val matcher: String) : Token()

            val tokens = mutableListOf<Token>()
            val current = StringBuilder()

            fun String.toToken(): Token = when (this) {
                "(", ")", "&", "|" -> OperatorToken(this)
                else -> VersionMatcherToken(this)
            }
            for (char in this) {
                if (char == ' ') {
                    continue
                }

                when (char) {
                    '(', ')', '&', '|' -> {
                        if (current.isNotEmpty()) {
                            tokens.add(current.toString().toToken())
                            current.clear()
                        }

                        tokens.add(char.toString().toToken())
                    }

                    else -> current.append(char)
                }
            }

            if (current.isNotEmpty()) {
                tokens.add(current.toString().toToken())
            }

            // 2. Parse.
            val stack = mutableListOf<VersionMatcher>()
            val operatorStack = mutableListOf<String>()

            // After tokenize, all possible tokens:
            // &, |, (, ) matcher.

            fun popUntilLeftParentheses() {
                while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
                    val operator = operatorStack.removeLast()
                    val right = stack.removeLast()
                    val left = stack.removeLast()

                    stack.add(
                        when (operator) {
                            "&" -> AndVersionMatcherImpl(left, right)
                            "|" -> OrVersionMatcherImpl(left, right)
                            else -> throw IllegalArgumentException("Invalid operator: $operator")
                        }
                    )
                }

                if (operatorStack.isNotEmpty()) {
                    operatorStack.removeLast()
                }
            }

            for (token in tokens) {
                when (token) {
                    is VersionMatcherToken -> stack.add(parseSingleVersionMatcher(token.matcher))
                    is OperatorToken -> when (token.operator) {
                        "(" -> operatorStack.add(token.operator)
                        ")" -> popUntilLeftParentheses()
                        "&", "|" -> {
                            while (operatorStack.isNotEmpty() && operatorStack.last() != "(") {
                                val operator = operatorStack.removeLast()
                                val right = stack.removeLast()
                                val left = stack.removeLast()

                                stack.add(
                                    when (operator) {
                                        "&" -> AndVersionMatcherImpl(left, right)
                                        "|" -> OrVersionMatcherImpl(left, right)
                                        else -> throw IllegalArgumentException("Invalid operator: $operator")
                                    }
                                )
                            }

                            operatorStack.add(token.operator)
                        }

                        else -> throw IllegalArgumentException("Invalid operator: ${token.operator}")
                    }
                }
            }

            while (operatorStack.isNotEmpty()) {
                val operator = operatorStack.removeLast()
                val right = stack.removeLast()
                val left = stack.removeLast()

                stack.add(
                    when (operator) {
                        "&" -> AndVersionMatcherImpl(left, right)
                        "|" -> OrVersionMatcherImpl(left, right)
                        else -> throw IllegalArgumentException("Invalid operator: $operator")
                    }
                )
            }

            require(stack.size == 1) {
                "Invalid version matcher: $this"
            }

            return stack.first()
        }
    }

    override fun createAndVersionMatcher(left: VersionMatcher, right: VersionMatcher): AndVersionMatcher =
        AndVersionMatcherImpl(left, right)

    override fun createOrVersionMatcher(left: VersionMatcher, right: VersionMatcher): OrVersionMatcher =
        OrVersionMatcherImpl(left, right)

    override fun createIncludeVersionMatcher(value: Version): IncludeVersionMatcher = IncludeVersionMatcherImpl(value)

    override fun createExcludeVersionMatcher(value: Version): ExcludeVersionMatcher = ExcludeVersionMatcherImpl(value)

    override fun createGreaterThanVersionMatcher(version: Version): GreaterThanVersionMatcher =
        GreaterThanVersionMatcherImpl(version)

    override fun createGreaterThanOrEqualVersionMatcher(version: Version): GreaterThanOrEqualVersionMatcher =
        GreaterThanOrEqualVersionMatcherImpl(version)

    override fun createLessThanVersionMatcher(version: Version): LessThanVersionMatcher =
        LessThanVersionMatcherImpl(version)

    override fun createLessThanOrEqualVersionMatcher(version: Version): LessThanOrEqualVersionMatcher =
        LessThanOrEqualVersionMatcherImpl(version)

    override fun createMajorVersionPrefixMatcher(major: Int): MajorVersionPrefixMatcher =
        MajorVersionPrefixMatcherImpl(major)

    override fun createMajorMinorVersionPrefixMatcher(major: Int, minor: Int): MajorMinorVersionPrefixMatcher =
        MajorMinorVersionPrefixMatcherImpl(major, minor)

    private val MAJOR_PREFIX_REGEX = """(\d+)\.\+""".toRegex()
    private val MAJOR_MINOR_PREFIX_REGEX = """(\d+)\.(\d+)\.\+""".toRegex()

    private fun parseSingleVersionMatcher(string: String): VersionMatcher {
        string.apply {
            require(isNotEmpty()) {
                "Version matcher string must not be empty."
            }

            return when {
                // Not equal.
                startsWith('!') || startsWith('~') -> ExcludeVersionMatcherImpl(substring(1).toVersion())

                // Greater than or equal.
                startsWith(">=") || startsWith("=>") -> GreaterThanOrEqualVersionMatcherImpl(substring(2).toVersion())
                startsWith(']') -> GreaterThanOrEqualVersionMatcherImpl(substring(1).toVersion())

                endsWith("<=") || endsWith("=<") -> GreaterThanOrEqualVersionMatcherImpl(
                    substring(
                        0,
                        length - 2
                    ).toVersion()
                )

                endsWith('[') -> GreaterThanOrEqualVersionMatcherImpl(substring(0, length - 1).toVersion())

                // Greater than.
                startsWith('>') -> GreaterThanVersionMatcherImpl(substring(1).toVersion())
                endsWith('<') -> GreaterThanVersionMatcherImpl(substring(0, length - 1).toVersion())

                // Less than or equal.
                startsWith("<=") || startsWith("=<") -> LessThanOrEqualVersionMatcherImpl(substring(2).toVersion())
                startsWith('[') -> LessThanOrEqualVersionMatcherImpl(substring(1).toVersion())

                endsWith(">=") || endsWith("=>") -> LessThanOrEqualVersionMatcherImpl(
                    substring(
                        0,
                        length - 2
                    ).toVersion()
                )

                endsWith(']') -> LessThanOrEqualVersionMatcherImpl(substring(0, length - 1).toVersion())

                // Less than.
                startsWith('<') -> LessThanVersionMatcherImpl(substring(1).toVersion())
                endsWith('>') -> LessThanVersionMatcherImpl(substring(0, length - 1).toVersion())

                // Prefix matcher.
                endsWith(".+") -> when {
                    matches(MAJOR_MINOR_PREFIX_REGEX) -> {
                        val (major, minor) = MAJOR_MINOR_PREFIX_REGEX.matchEntire(this)!!.destructured
                        MajorMinorVersionPrefixMatcherImpl(major.toInt(), minor.toInt())
                    }

                    matches(MAJOR_PREFIX_REGEX) -> {
                        val (major) = MAJOR_PREFIX_REGEX.matchEntire(this)!!.destructured
                        MajorVersionPrefixMatcherImpl(major.toInt())
                    }

                    else -> throw IllegalArgumentException("Invalid version prefix: $this")
                }

                else -> IncludeVersionMatcherImpl(toVersion())
            }
        }
    }

    // Version

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

    override fun createVersion(major: Int, minor: Int, patch: Int, preRelease: String?, build: String?): Version {
        return VersionImpl(major, minor, patch, preRelease, build)
    }

    override fun parseVersion(string: String): Version = VERSION_STRING_REGEX.matchEntire(string)?.let { it ->
        val (major, minor, patch, preRelease, build) = it.destructured
        VersionImpl(
            major.toInt(), minor.toInt(), patch.toInt(),
            preRelease.takeIf { it.isNotEmpty() }, build.takeIf { it.isNotEmpty() }
        )
    } ?: throw IllegalArgumentException(
        "Invalid version string: '$this', " +
                "make sure it matches the regex from the semantic versioning 2.0.0: $VERSION_STRING_REGEX."
    )
}