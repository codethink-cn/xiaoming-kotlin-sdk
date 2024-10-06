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


@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.FIELD_VERSION
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.common.getValue
import cn.codethink.xiaoming.common.tristateOf
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.set
import com.fasterxml.jackson.annotation.JsonTypeName

const val PERMISSION_COMPARATOR_TYPE_DEFAULT = "default"

/**
 * Comparator to match the permission subject, id and contexts.
 *
 * The comparator will check if given permission node is matched with the
 * record node. It consists of matching subject and node. If both matched,
 * the comparator will return the [value] of the record.
 *
 * @author Chuanwise
 * @see DefaultPermissionComparatorV1
 */
@JsonTypeName(PERMISSION_COMPARATOR_TYPE_DEFAULT)
interface DefaultPermissionComparator : PermissionComparator {
    val version: String
    val subject: Matcher<SubjectDescriptor>
    val node: Matcher<SegmentId>
    val value: Boolean?
}

const val DEFAULT_PERMISSION_COMPARATOR_VERSION_1 = "1"
const val DEFAULT_PERMISSION_COMPARATOR_FIELD_VALUE = "value"

const val PERMISSION_COMPARATOR_FIELD_SUBJECT = "subject"
const val PERMISSION_COMPARATOR_FIELD_NODE = "node"

@JsonTypeName(DEFAULT_PERMISSION_COMPARATOR_VERSION_1)
class DefaultPermissionComparatorV1 : AbstractData, DefaultPermissionComparator {
    override val type: String by raw

    override val version: String by raw
    override val value: Boolean? by raw

    override val subject: Matcher<SubjectDescriptor> by raw
    override val node: Matcher<SegmentId> by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        subject: Matcher<SubjectDescriptor>,
        node: Matcher<SegmentId>,
        value: Boolean?,
        raw: Raw = MapRaw()
    ) : super(raw) {
        raw[FIELD_TYPE] = PERMISSION_COMPARATOR_TYPE_DEFAULT
        raw[FIELD_VERSION] = DEFAULT_PERMISSION_COMPARATOR_VERSION_1

        raw[PERMISSION_COMPARATOR_FIELD_SUBJECT] = subject
        raw[PERMISSION_COMPARATOR_FIELD_NODE] = node
        raw[DEFAULT_PERMISSION_COMPARATOR_FIELD_VALUE] = value
    }

    override fun compare(context: PermissionComparingContext): Tristate? {
        if (!subject.isMatched(context.permission.descriptor.subject)) {
            return null
        }
        context.permission.descriptor.subject

        if (!node.isMatched(context.permission.descriptor.node)) {
            return null
        }
        return tristateOf(value)
    }
}