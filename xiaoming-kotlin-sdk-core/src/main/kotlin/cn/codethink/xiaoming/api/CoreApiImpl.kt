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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.event.EventPublishPolicy
import cn.codethink.xiaoming.event.EventPublishPolicyImpl
import cn.codethink.xiaoming.permission.PluginRequirementImpl
import cn.codethink.xiaoming.plugin.PluginRequirement
import cn.codethink.xiaoming.plugin.PluginStateChangePolicy
import cn.codethink.xiaoming.plugin.PluginStateChangePolicyImpl
import cn.codethink.xiaoming.serialization.CodecResolver
import cn.codethink.xiaoming.serialization.CodecResolverInitializer
import cn.codethink.xiaoming.serialization.CoreCodecResolverInitializeContextImpl
import cn.codethink.xiaoming.util.AndVersionMatcher
import cn.codethink.xiaoming.util.AndVersionMatcherImpl
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.CauseImpl
import cn.codethink.xiaoming.util.Data
import cn.codethink.xiaoming.util.DataImpl
import cn.codethink.xiaoming.util.EmptyStoreImpl
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
import cn.codethink.xiaoming.util.KebabCaseNamingPolicy
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcher
import cn.codethink.xiaoming.util.LessThanOrEqualVersionMatcherImpl
import cn.codethink.xiaoming.util.LessThanVersionMatcher
import cn.codethink.xiaoming.util.LessThanVersionMatcherImpl
import cn.codethink.xiaoming.util.LiteralSegmentIdPatternElement
import cn.codethink.xiaoming.util.LiteralSegmentIdPatternElementImpl
import cn.codethink.xiaoming.util.LongIdImpl
import cn.codethink.xiaoming.util.LowerCamelCaseNamingPolicy
import cn.codethink.xiaoming.util.LowerCaseNamingPolicy
import cn.codethink.xiaoming.util.LowerDotCaseNamingPolicy
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorMinorVersionPrefixMatcherImpl
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcher
import cn.codethink.xiaoming.util.MajorVersionPrefixMatcherImpl
import cn.codethink.xiaoming.util.MapStoreImpl
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.NamespaceIdImpl
import cn.codethink.xiaoming.util.NamespaceIdPattern
import cn.codethink.xiaoming.util.NamespaceIdPatternImpl
import cn.codethink.xiaoming.util.NamingPolicy
import cn.codethink.xiaoming.util.NumericalId
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.OperationImpl
import cn.codethink.xiaoming.util.OrVersionMatcher
import cn.codethink.xiaoming.util.OrVersionMatcherImpl
import cn.codethink.xiaoming.util.ReadOnlyStorePropertyImpl
import cn.codethink.xiaoming.util.ReadWriteStorePropertyImpl
import cn.codethink.xiaoming.util.RegexSegmentIdPatternElement
import cn.codethink.xiaoming.util.RegexSegmentIdPatternElementImpl
import cn.codethink.xiaoming.util.SegmentId
import cn.codethink.xiaoming.util.SegmentIdImpl
import cn.codethink.xiaoming.util.SegmentIdPattern
import cn.codethink.xiaoming.util.SegmentIdPatternElement
import cn.codethink.xiaoming.util.SegmentIdPatternImpl
import cn.codethink.xiaoming.util.SnakeCaseNamingPolicy
import cn.codethink.xiaoming.util.Store
import cn.codethink.xiaoming.util.StringId
import cn.codethink.xiaoming.util.StringIdImpl
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.TemplateImpl
import cn.codethink.xiaoming.util.TextualId
import cn.codethink.xiaoming.util.Time
import cn.codethink.xiaoming.util.TimeImpl
import cn.codethink.xiaoming.util.TypeMeta
import cn.codethink.xiaoming.util.TypeMetaImpl
import cn.codethink.xiaoming.util.UniversalUniqueId
import cn.codethink.xiaoming.util.UniversalUniqueIdImpl
import cn.codethink.xiaoming.util.UpperCamelCaseNamingPolicy
import cn.codethink.xiaoming.util.UpperSnakeCaseNamingPolicy
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionImpl
import cn.codethink.xiaoming.util.VersionMatcher
import cn.codethink.xiaoming.util.WildCardSegmentIdPatternElement
import cn.codethink.xiaoming.util.toNamespaceId
import cn.codethink.xiaoming.util.toSegmentId
import cn.codethink.xiaoming.util.toSegmentIdPattern
import cn.codethink.xiaoming.util.toVersion
import cn.codethink.xiaoming.util.toVersionMatcher
import org.apache.commons.text.StringEscapeUtils
import java.lang.reflect.Type
import java.util.ServiceLoader
import java.util.UUID
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

@InternalApi
class CoreApiImpl : CoreApi {
    // Id
    override fun parseTextualId(string: String): TextualId {
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
    override fun createTemplate(format: String): Template {
        return TemplateImpl(format)
    }

    // Cause
    override fun createCause(description: String, cause: Cause?): Cause {
        return CauseImpl(description, cause)
    }

    // Operation
    override fun createOperation(
        message: String,
        operator: SubjectDescriptor,
        cause: Cause?,
        time: Time,
        id: Id
    ): Operation {
        return OperationImpl(message, cause, operator, time, id)
    }

    // EventPublishPolicy
    override fun createEventPublishPolicy(mutable: Boolean, interceptable: Boolean): EventPublishPolicy {
        return EventPublishPolicyImpl.of(mutable, interceptable)
    }

    // Data
    override fun createData(raw: MutableStore): Data {
        return DataImpl(raw)
    }

    // Time
    override fun createUnixMillisecondsTime(milliseconds: Long): Time = TimeImpl(milliseconds)

    override fun createUnixSecondsTime(seconds: Long): Time = TimeImpl(seconds * 1000)

    // PluginMetaMatcher
    override fun createPluginRequirement(string: String): PluginRequirement {
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

        val pluginId = NamespaceId(group, name)

        val version = if (length == colonIndexAfterName + 1) null else {
            string.substring(colonIndexAfterName + 1, length).toVersionMatcher()
        }

        return PluginRequirementImpl(pluginId, version, optional, local)
    }

    override fun createPluginRequirement(
        id: NamespaceId,
        version: VersionMatcher?,
        optional: Boolean,
        local: Boolean
    ): PluginRequirement {
        return PluginRequirementImpl(id, version, optional, local)
    }

    private fun String.toSegmentIdPatternElement(): SegmentIdPatternElement {
        if (isEmpty()) {
            throw IllegalArgumentException("String matcher should not be empty.")
        }

        when (this) {
            "+" -> return WildCardSegmentIdPatternElement.REQUIRED
            "?" -> return WildCardSegmentIdPatternElement.OPTIONAL
            "++" -> return WildCardSegmentIdPatternElement.GREEDY_REQUIRED
            "??", "*" -> return WildCardSegmentIdPatternElement.GREEDY_OPTIONAL
            else -> {
                if (startsWith("{") && endsWith("}")) {
                    val pattern = substring(1, length - 1)
                    if (pattern.isEmpty()) {
                        throw IllegalArgumentException("Empty regex string matcher.")
                    }
                    return RegexSegmentIdPatternElementImpl(Regex(pattern))
                }
                if (startsWith("\"") && endsWith("\"")) {
                    val unescaped = StringEscapeUtils.unescapeJson(substring(1, length - 1))
                    if (unescaped.isEmpty()) {
                        throw IllegalArgumentException("Empty literal string matcher.")
                    }
                    return LiteralSegmentIdPatternElementImpl(unescaped)
                }
                return LiteralSegmentIdPatternElementImpl(this)
            }
        }
    }

    override fun createLiteralSegmentIdPatternElement(string: String): LiteralSegmentIdPatternElement {
        return LiteralSegmentIdPatternElementImpl(string)
    }

    override fun createRegexStringMatcher(regex: Regex): RegexSegmentIdPatternElement {
        return RegexSegmentIdPatternElementImpl(regex)
    }

    // SegmentIdPattern
    override fun createSegmentIdPattern(string: String): SegmentIdPattern {
        if (string.isEmpty()) {
            throw IllegalArgumentException("Segment matcher should not be empty.")
        }

        val matchers = mutableListOf<SegmentIdPatternElement>()
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
                            matchers.add(stringBuilder.toString().toSegmentIdPatternElement())
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
                matchers.add(stringBuilder.toString().toSegmentIdPatternElement())
            }
        }

        return SegmentIdPatternImpl(matchers)
    }

    override fun createSegmentIdPattern(matchers: List<SegmentIdPatternElement>): SegmentIdPattern {
        return SegmentIdPatternImpl(matchers)
    }

    // NamespaceIdMatcher
    override fun createNamespaceIdMatcher(group: SegmentIdPattern, name: SegmentIdPattern): NamespaceIdPattern {
        return NamespaceIdPatternImpl(group, name)
    }

    override fun parseNamespaceIdMatcher(string: String): NamespaceIdPattern {
        if (string.isEmpty()) {
            throw IllegalArgumentException("Namespace id matcher should not be empty.")
        }

        val colonIndex = string.indexOf(':')
        require(colonIndex != -1) { "Namespace id matcher string should contain a colon." }

        val group = string.substring(0, colonIndex).toSegmentIdPattern()
        val name = string.substring(colonIndex + 1).toSegmentIdPattern()

        return NamespaceIdPatternImpl(group, name)
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
        "Invalid version string: '$string', " +
                "make sure it matches the regex from the semantic versioning 2.0.0: $VERSION_STRING_REGEX."
    )

    // MapRaw
    override fun createMapStore(map: MutableMap<String, Any?>): MutableStore {
        return MapStoreImpl(map)
    }

    override fun createEmptyStore(): Store {
        return EmptyStoreImpl
    }

    override fun <T> createReadOnlyStoreProperty(
        store: Store,
        name: String?,
        meta: TypeMeta<T>?,
        namingPolicy: NamingPolicy?,
        defaultValueFactory: Supplier<T>?
    ): ReadOnlyProperty<Any?, T> = ReadOnlyStorePropertyImpl(store, name, meta, namingPolicy, defaultValueFactory)

    override fun <T> createReadWriteStoreProperty(
        store: MutableStore,
        name: String?,
        meta: TypeMeta<T>?,
        namingPolicy: NamingPolicy?,
        defaultValueFactory: Supplier<T>?
    ): ReadWriteProperty<Any?, T> = ReadWriteStorePropertyImpl(store, name, meta, namingPolicy, defaultValueFactory)

    // TypeMeta
    private val anyRequiredNullableTypeMeta = TypeMetaImpl<Any?>(Any::class.java, nullable = true)

    override fun createTypeMeta(type: Type, nullable: Boolean): TypeMeta<*> {
        if (type == Any::class.java && nullable) {
            return anyRequiredNullableTypeMeta
        }

        return TypeMetaImpl<Any>(type, nullable)
    }

    // NamingPolicy
    override fun getKebabCaseNamingPolicy(): NamingPolicy = KebabCaseNamingPolicy

    override fun getLowerCamelCaseNamingPolicy(): NamingPolicy = LowerCamelCaseNamingPolicy

    override fun getUpperCamelCaseNamingPolicy(): NamingPolicy = UpperCamelCaseNamingPolicy

    override fun getSnakeCaseNamingPolicy(): NamingPolicy = SnakeCaseNamingPolicy

    override fun getUpperSnakeCaseNamingPolicy(): NamingPolicy = UpperSnakeCaseNamingPolicy

    override fun getLowerCaseNamingPolicy(): NamingPolicy = LowerCaseNamingPolicy

    override fun getLowerDotCaseNamingPolicy(): NamingPolicy = LowerDotCaseNamingPolicy

    // PluginStateChangePolicy
    override fun createPluginStateChangePolicy(ignorePreviousError: Boolean, ignoreCurrentError: Boolean): PluginStateChangePolicy {
        return PluginStateChangePolicyImpl.of(ignorePreviousError, ignoreCurrentError)
    }

    // CodecResolver
    override fun findAndApplyInitializers(resolver: CodecResolver, operation: Operation, classLoader: ClassLoader?, replace: Boolean, visible: Boolean) {
        val initializerClass = CodecResolverInitializer::class.java
        val loader = when (classLoader) {
            null -> ServiceLoader.load(initializerClass)
            else -> ServiceLoader.load(initializerClass, classLoader)
        }

        val context = CoreCodecResolverInitializeContextImpl(resolver, operation, replace, visible)
        for (initializer in loader) {
            initializer.initialize(context)
        }
    }
}