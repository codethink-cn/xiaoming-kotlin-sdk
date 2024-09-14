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
import cn.codethink.xiaoming.common.DEFAULT_PERMISSION_MATCHER_FIELD_CONTEXT
import cn.codethink.xiaoming.common.DEFAULT_PERMISSION_MATCHER_FIELD_NODE
import cn.codethink.xiaoming.common.LITERAL_PERMISSION_MATCHER_FIELD_PERMISSION
import cn.codethink.xiaoming.common.LiteralMatcher
import cn.codethink.xiaoming.common.MATCHER_FIELD_TYPE
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.PERMISSION_CONTEXT_META_FIELD_DEFAULT_VALUE
import cn.codethink.xiaoming.common.PERMISSION_CONTEXT_META_FIELD_DESCRIPTION
import cn.codethink.xiaoming.common.PERMISSION_CONTEXT_META_FIELD_NULLABLE
import cn.codethink.xiaoming.common.PERMISSION_CONTEXT_META_FIELD_OPTIONAL
import cn.codethink.xiaoming.common.PERMISSION_FIELD_CONTEXT
import cn.codethink.xiaoming.common.PERMISSION_FIELD_DESCRIPTOR
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_DEFAULT
import cn.codethink.xiaoming.common.PERMISSION_MATCHER_TYPE_LITERAL
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_CONTEXT
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_DESCRIPTION
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_DESCRIPTOR
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_ID
import cn.codethink.xiaoming.common.PERMISSION_META_FIELD_SUBJECT
import cn.codethink.xiaoming.common.PERMISSION_SUBJECT_FIELD_ID
import cn.codethink.xiaoming.common.PERMISSION_SUBJECT_FIELD_SUBJECT
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.defaultNullable
import cn.codethink.xiaoming.common.defaultOptional
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.RawValue
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

/**
 * Describe a permission.
 *
 * @author Chuanwise
 */
class PermissionDescriptor(
    raw: Raw
) : AbstractData(raw) {
    val id: SegmentId by raw
    val subject: Subject by raw

    @JvmOverloads
    constructor(
        id: SegmentId,
        subject: Subject,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_SUBJECT_FIELD_ID] = id
        raw[PERMISSION_SUBJECT_FIELD_SUBJECT] = subject
    }
}

/**
 * Permission context is a variable that can be used to test if a permission is available.
 *
 * @author Chuanwise
 */
class PermissionContextMeta(
    raw: Raw
) : AbstractData(raw) {
    @RawValue(PERMISSION_CONTEXT_META_FIELD_DEFAULT_VALUE)
    val defaultValue: Any? by raw
    val description: String? by raw

    val optional: Boolean by raw
    val nullable: Boolean by raw

    @JvmOverloads
    constructor(
        defaultValue: Any?,
        description: String?,
        optional: Boolean,
        nullable: Boolean,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_CONTEXT_META_FIELD_DEFAULT_VALUE] = defaultValue
        raw[PERMISSION_CONTEXT_META_FIELD_DESCRIPTION] = description
        raw[PERMISSION_CONTEXT_META_FIELD_OPTIONAL] = optional
        raw[PERMISSION_CONTEXT_META_FIELD_NULLABLE] = nullable
    }
}

inline fun <reified T> PermissionContextMeta(
    defaultValue: T? = null,
    description: String? = null,
    optional: Boolean = defaultOptional<T>(),
    nullable: Boolean = defaultNullable<T>(),
    raw: Raw = MapRaw()
) = PermissionContextMeta(defaultValue, description, optional, nullable, raw)

/**
 * Describe a permission.
 *
 * @author Chuanwise
 */
class PermissionMeta(
    raw: Raw
) : AbstractData(raw) {
    val id: SegmentId by raw
    val subject: Subject by raw
    val context: Map<String, PermissionContextMeta> by raw
    val description: String? by raw
    val descriptor: PermissionDescriptor by raw

    @JvmOverloads
    constructor(
        id: SegmentId,
        subject: Subject,
        context: Map<String, PermissionContextMeta> = emptyMap(),
        description: String? = null,
        descriptor: PermissionDescriptor = PermissionDescriptor(id, subject),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_META_FIELD_ID] = id
        raw[PERMISSION_META_FIELD_SUBJECT] = subject
        raw[PERMISSION_META_FIELD_DESCRIPTION] = description
        raw[PERMISSION_META_FIELD_CONTEXT] = context
        raw[PERMISSION_META_FIELD_DESCRIPTOR] = descriptor
    }
}

/**
 * Permission is a specific permission node that can be used to test if an [Subject].
 *
 * @author Chuanwise
 */
class Permission(
    raw: Raw
) : AbstractData(raw) {
    val descriptor: PermissionDescriptor by raw
    val context: Map<String, Any?> by raw

    @JvmOverloads
    constructor(
        descriptor: PermissionDescriptor,
        context: Map<String, Any?> = emptyMap(),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[PERMISSION_FIELD_DESCRIPTOR] = descriptor
        raw[PERMISSION_FIELD_CONTEXT] = context
    }
}

class LiteralPermissionMatcher(
    raw: Raw
) : AbstractData(raw), LiteralMatcher<Permission> {
    override val type: String by raw
    override val value: Permission by raw

    @JvmOverloads
    constructor(
        permission: Permission,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = PERMISSION_MATCHER_TYPE_LITERAL
        raw[LITERAL_PERMISSION_MATCHER_FIELD_PERMISSION] = permission
    }
}

fun Permission.toLiteralMatcher(): LiteralPermissionMatcher = LiteralPermissionMatcher(this)

class DefaultPermissionMatcher(
    raw: Raw
) : AbstractData(raw), Matcher<Permission> {
    override val type: String by raw

    val node: Matcher<SegmentId> by raw
    val context: Map<String, Matcher<*>> by raw

    @JvmOverloads
    constructor(
        node: Matcher<SegmentId>,
        context: Map<String, Matcher<*>> = emptyMap(),
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = PERMISSION_MATCHER_TYPE_DEFAULT
        raw[DEFAULT_PERMISSION_MATCHER_FIELD_NODE] = node
        raw[DEFAULT_PERMISSION_MATCHER_FIELD_CONTEXT] = context
    }

    @Suppress("UNCHECKED_CAST")
    override fun isMatched(target: Permission): Boolean {
        if (!node.isMatched(target.descriptor.id)) {
            return false
        }

        for ((key, matcher) in context) {
            val value = target.context[key] ?: return false
            if (!(matcher as Matcher<Any?>).isMatched(value)) {
                return false
            }
        }

        return true
    }
}