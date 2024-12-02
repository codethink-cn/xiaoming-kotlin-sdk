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

import cn.codethink.xiaoming.permission.Permission
import cn.codethink.xiaoming.permission.PermissionMatcher
import com.fasterxml.jackson.annotation.JsonTypeName

const val PERMISSION_MATCHER_TYPE_LITERAL = "permission.literal"

@JsonTypeName(PERMISSION_MATCHER_TYPE_LITERAL)
class LiteralPermissionMatcher : AbstractData, PermissionMatcher, LiteralMatcher<Permission> {
    private var type: String by raw
    override var value: Permission by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        value: Permission,
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.type = PERMISSION_MATCHER_TYPE_LITERAL
        this.value = value
    }
}

const val PERMISSION_MATCHER_TYPE_DEFAULT = "permission.default"

@JsonTypeName(PERMISSION_MATCHER_TYPE_DEFAULT)
class DefaultPermissionMatcher : AbstractData, PermissionMatcher {
    private var type: String by raw

    private var id: Matcher<Id> by raw
    private var arguments: Map<String, Matcher<*>> by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Matcher<Id>,
        arguments: Map<String, Matcher<*>> = emptyMap(),
        raw: Raw = MapRaw()
    ) : super(raw) {
        this.type = PERMISSION_MATCHER_TYPE_DEFAULT
        this.id = id
        this.arguments = arguments
    }

    override fun isMatched(target: Permission): Boolean {
        if (!id.isMatched(target.descriptor.id)) {
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