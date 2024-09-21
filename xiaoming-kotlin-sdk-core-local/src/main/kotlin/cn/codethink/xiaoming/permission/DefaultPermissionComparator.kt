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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.FIELD_TYPE
import cn.codethink.xiaoming.common.FIELD_VERSION
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.SegmentId
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.common.tristateOf
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.RawValue
import cn.codethink.xiaoming.io.data.getValue
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
    override val type: String
        get() = PERMISSION_COMPARATOR_TYPE_DEFAULT
    val version: String
    val subjectMatcher: Matcher<Subject>
    val nodeMatcher: Matcher<SegmentId>
    val value: Boolean?
}

const val DEFAULT_PERMISSION_COMPARATOR_VERSION_1 = "1"
const val DEFAULT_PERMISSION_COMPARATOR_FIELD_VALUE = "value"

const val PERMISSION_COMPARATOR_FIELD_SUBJECT_MATCHER = "subject_matcher"
const val PERMISSION_COMPARATOR_FIELD_NODE_MATCHER = "node_matcher"

@JsonTypeName(DEFAULT_PERMISSION_COMPARATOR_VERSION_1)
class DefaultPermissionComparatorV1(
    raw: Raw
) : AbstractData(raw), DefaultPermissionComparator {
    override val version: String by raw
    override val value: Boolean? by raw

    @RawValue(PERMISSION_COMPARATOR_FIELD_SUBJECT_MATCHER)
    override val subjectMatcher: Matcher<Subject> by raw

    @RawValue(PERMISSION_COMPARATOR_FIELD_NODE_MATCHER)
    override val nodeMatcher: Matcher<SegmentId> by raw

    @JvmOverloads
    constructor(
        subjectMatcher: Matcher<Subject>,
        nodeMatcher: Matcher<SegmentId>,
        value: Boolean?,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = PERMISSION_COMPARATOR_TYPE_DEFAULT
        raw[FIELD_VERSION] = DEFAULT_PERMISSION_COMPARATOR_VERSION_1

        raw[PERMISSION_COMPARATOR_FIELD_SUBJECT_MATCHER] = subjectMatcher
        raw[PERMISSION_COMPARATOR_FIELD_NODE_MATCHER] = nodeMatcher
        raw[DEFAULT_PERMISSION_COMPARATOR_FIELD_VALUE] = value
    }

    override fun compare(context: PermissionComparingContext): Tristate? {
        if (!subjectMatcher.isMatched(context.permission.descriptor.subject)) {
            return null
        }
        if (!nodeMatcher.isMatched(context.permission.descriptor.node)) {
            return null
        }
        return tristateOf(value)
    }
}