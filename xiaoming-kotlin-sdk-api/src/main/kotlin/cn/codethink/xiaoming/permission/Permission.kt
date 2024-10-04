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

@file:JvmName("Permissions")

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.IdSubjectDescriptor
import cn.codethink.xiaoming.common.LITERAL_PERMISSION_MATCHER_FIELD_VALUE
import cn.codethink.xiaoming.common.LiteralMatcher
import cn.codethink.xiaoming.common.MATCHER_FIELD_TYPE
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.PERMISSION_FIELD_ARGUMENTS
import cn.codethink.xiaoming.common.PERMISSION_FIELD_DESCRIPTOR
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_DEFAULT
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_LITERAL
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_DESCRIPTION
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_DESCRIPTOR
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_NODE
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_PARAMETERS
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_SUBJECT
import cn.codethink.xiaoming.common.PERMISSION_SUBJECT_DESCRIPTOR_FIELD_NODE
import cn.codethink.xiaoming.common.PERMISSION_SUBJECT_DESCRIPTOR_FIELD_SUBJECT
import cn.codethink.xiaoming.common.PERMISSION_VARIABLE_META_FIELD_DEFAULT_MATCHER_OR_VALUE
import cn.codethink.xiaoming.common.PERMISSION_VARIABLE_META_FIELD_DESCRIPTION
import cn.codethink.xiaoming.common.PERMISSION_VARIABLE_META_FIELD_NULLABLE
import cn.codethink.xiaoming.common.PERMISSION_VARIABLE_META_FIELD_OPTIONAL
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.defaultNullable
import cn.codethink.xiaoming.common.defaultOptional
import cn.codethink.xiaoming.io.data.Field
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.io.data.setValue
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Describe a permission.
 *
 * @author Chuanwise
 */
class PermissionDescriptor(
    raw: Raw
) : AbstractData(raw) {
    val node: SegmentId by raw
    val subject: IdSubjectDescriptor by raw

    @JvmOverloads
    constructor(
        node: SegmentId,
        subject: IdSubjectDescriptor,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_SUBJECT_DESCRIPTOR_FIELD_NODE] = node
        raw[PERMISSION_SUBJECT_DESCRIPTOR_FIELD_SUBJECT] = subject
    }
}

/**
 * Permission context is a variable that can be used to test if a permission is available.
 *
 * @author Chuanwise
 */
class PermissionParameterMeta(
    raw: Raw
) : AbstractData(raw) {
    @Field(PERMISSION_VARIABLE_META_FIELD_DEFAULT_MATCHER_OR_VALUE)
    val defaultMatcherOrValue: Any? by raw
    val description: String? by raw

    val optional: Boolean by raw
    val nullable: Boolean by raw

    @JvmOverloads
    constructor(
        defaultMatcherOrValue: Any?,
        description: String?,
        optional: Boolean,
        nullable: Boolean,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_VARIABLE_META_FIELD_DEFAULT_MATCHER_OR_VALUE] = defaultMatcherOrValue
        raw[PERMISSION_VARIABLE_META_FIELD_DESCRIPTION] = description
        raw[PERMISSION_VARIABLE_META_FIELD_OPTIONAL] = optional
        raw[PERMISSION_VARIABLE_META_FIELD_NULLABLE] = nullable

        if (optional && !nullable && defaultMatcherOrValue == null) {
            throw IllegalArgumentException(
                "If a permission parameter is optional, it must be nullable or have a non-null default matcher or value."
            )
        }
    }
}

inline fun <reified T> PermissionParameterMeta(
    defaultMatcherOrValue: T? = null,
    description: String? = null,
    optional: Boolean = defaultOptional<T>(),
    nullable: Boolean = defaultNullable<T>(),
    raw: Raw = MapRaw()
) = PermissionParameterMeta(defaultMatcherOrValue, description, optional, nullable, raw)

/**
 * Describe a permission.
 *
 * @author Chuanwise
 */
class PermissionMeta(
    raw: Raw
) : AbstractData(raw) {
    val node: SegmentId by raw
    val subject: IdSubjectDescriptor by raw
    val parameters: Map<String, PermissionParameterMeta> by raw
    val description: String? by raw
    val descriptor: PermissionDescriptor by raw

    @JvmOverloads
    constructor(
        node: SegmentId,
        subject: IdSubjectDescriptor,
        parameters: Map<String, PermissionParameterMeta> = emptyMap(),
        description: String? = null,
        descriptor: PermissionDescriptor = PermissionDescriptor(node, subject),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_META_FIELD_NODE] = node
        raw[PERMISSION_META_FIELD_SUBJECT] = subject
        raw[PERMISSION_META_FIELD_DESCRIPTION] = description
        raw[PERMISSION_META_FIELD_PARAMETERS] = parameters
        raw[PERMISSION_META_FIELD_DESCRIPTOR] = descriptor
    }
}

/**
 * Permission is a specific permission node that can be used to test if an [SubjectDescriptor].
 *
 * @author Chuanwise
 */
class Permission(
    raw: Raw
) : AbstractData(raw) {
    val descriptor: PermissionDescriptor by raw
    val arguments: Map<String, Any?> by raw

    @JvmOverloads
    constructor(
        descriptor: PermissionDescriptor,
        arguments: Map<String, Any?> = emptyMap(),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_FIELD_DESCRIPTOR] = descriptor
        raw[PERMISSION_FIELD_ARGUMENTS] = arguments
    }
}

/**
 * Matches if given permission is exactly the same as the [value].
 *
 * @author Chuanwise
 */
@JsonTypeName(PERMISSION_MATCHER_TYPE_LITERAL)
class LiteralPermissionMatcher(
    raw: Raw
) : AbstractData(raw), LiteralMatcher<Permission> {
    override val type: String by raw

    @JsonIgnore
    override val targetType: Class<Permission> = Permission::class.java

    @JsonIgnore
    override val targetNullable: Boolean = false

    override val value: Permission by raw

    @JvmOverloads
    constructor(
        value: Permission,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = PERMISSION_MATCHER_TYPE_LITERAL
        raw[LITERAL_PERMISSION_MATCHER_FIELD_VALUE] = value
    }
}

fun Permission.toLiteralMatcher(): LiteralPermissionMatcher = LiteralPermissionMatcher(this)

@JsonTypeName(PERMISSION_MATCHER_TYPE_DEFAULT)
class DefaultPermissionMatcher(
    raw: Raw
) : AbstractData(raw), Matcher<Permission> {
    override val type: String by raw

    @JsonIgnore
    override val targetType: Class<Permission> = Permission::class.java

    @JsonIgnore
    override val targetNullable: Boolean = false

    var node: Matcher<SegmentId> by raw
    var arguments: Map<String, Matcher<*>> by raw

    @JvmOverloads
    constructor(
        node: Matcher<SegmentId>,
        arguments: Map<String, Matcher<*>> = emptyMap(),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = PERMISSION_MATCHER_TYPE_DEFAULT
        this.node = node
        this.arguments = arguments
    }

    override fun isMatched(target: Permission): Boolean {
        if (!node.isMatched(target.descriptor.node)) {
            return false
        }

        for ((key, matcher) in arguments) {
            val value = target.arguments[key] ?: return false
            if (!(matcher as Matcher<Any?>).isMatched(value)) {
                return false
            }
        }

        return true
    }
}