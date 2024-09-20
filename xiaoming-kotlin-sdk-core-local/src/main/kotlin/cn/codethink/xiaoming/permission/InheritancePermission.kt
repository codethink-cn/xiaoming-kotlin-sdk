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

import cn.codethink.xiaoming.common.toSegmentId

/**
 * Check permission node `inheritance` with arguments from:
 *
 * 1. subject or profile:
 *     1. `subject_id: Long` or `subject: String`: inherited from another subject(s).
 *     2. `profile_id: Long`: inherited from another profile(s).
 * 2. `context: Map<String, Any?> = emptyMap()`.
 * 3. `argument_matchers: Map<String, Matcher<Any?>> = emptyMap()`.
 *
 * For example, if profile with id `114514` has permission `a.b.c`, and another
 * profile with id `1919810` has `inheritance(profile_id = 114514)`, then `1919810`
 * will inherit all permissions from `114514`, also includes `a.b.c`.
 *
 * @author Chuanwise
 */
object InheritancePermissionComparator : PermissionComparator {
    private val INHERITANCE_PERMISSION_NODE = "inheritance".toSegmentId()

    override fun compare(context: PermissionComparingContext): Boolean {
        if (!context.record.nodeMatcher.isMatched(INHERITANCE_PERMISSION_NODE)) {
            throw IllegalArgumentException("The node of inheritance permission record should be $INHERITANCE_PERMISSION_NODE.")
        }

        TODO()
    }
}