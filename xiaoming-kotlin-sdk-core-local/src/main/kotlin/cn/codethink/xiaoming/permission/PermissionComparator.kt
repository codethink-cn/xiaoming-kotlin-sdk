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

import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.isMatchedOrNull
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord

data class PermissionComparingContext(
    val profile: PermissionProfile,
    val permission: Permission,
    val record: PermissionRecord,
    val api: LocalPermissionServiceApi
)


interface PermissionComparator {
    fun compare(context: PermissionComparingContext): Boolean
}

/**
 * Comparator to match the permission subject, id and contexts.
 *
 * The comparator will first check if given permission node is matched with the
 * record node. For example, record node `a.b.*` matches given `a.b.c`, but record
 * node `a.*` doesn't match `a.b.c` by default. Details see [DefaultPermissionMatcher].
 *
 * Then, the comparator will check if the permission context is matched with the
 * record context. If the permission meta is registered, the context will be checked
 * according it. Otherwise, the context will be checked in the most strict way.
 *
 * @author Chuanwise
 * @see PermissionComparator
 * @see DefaultPermissionMatcher
 * @see PermissionMeta
 * @see PermissionContextMeta
 */
object DefaultPermissionComparator : PermissionComparator {
    @Suppress("UNCHECKED_CAST")
    override fun compare(context: PermissionComparingContext): Boolean {
        val matched = context.record.node.isMatched(context.permission.descriptor.node)
        if (!matched) {
            return false
        }

        val meta = context.api.permissionMetas[context.permission.descriptor.node]?.value
        if (meta == null) {
            // Given permission context keys must equals to record context keys.
            if (context.record.context.keys != context.permission.context.keys) {
                return false
            }

            // Check if the context values are all matched.
            context.permission.context.forEach { (key, value) ->
                val matcher = context.record.context[key] as Matcher<Any?>?
                    ?: throw IllegalStateException("No matcher found for key: $key.")

                val result = matcher.isMatchedOrNull(value)
                if (result != null && !result) {
                    return false
                }
            }
        } else {
            // Given permission context keys contained in meta context.
            if (!meta.context.keys.containsAll(context.permission.context.keys)) {
                return false
            }
            if (!meta.context.keys.containsAll(context.record.context.keys)) {
                context.api.logger.warn {
                    "Redundant context keys: keys ${context.record.context.keys} are not all contained " +
                            "in meta context keys ${meta.context.keys} " +
                            "in comparing ${context.permission} and ${context.record}."
                }
            }

            // Check if the context values are all matched.
            meta.context.forEach { (key, contextMeta) ->
                val matcher = context.record.context[key] as Matcher<Any?>?
                val given = context.permission.context[key]

                if (given == null && context.permission.context.containsKey(key)) {
                    // If given is null, check `nullable`.
                    if (!contextMeta.nullable) {
                        return false
                    }
                    if (matcher == null) {
                        context.api.logger.warn {
                            "Context value $key is null, and it is nullable, but no matcher provided " +
                                    "in comparing ${context.permission} and ${context.record}."
                        }
                        return false
                    }

                    val result = matcher.isMatchedOrNull(null)
                    if (result == null || !result) {
                        return false
                    }
                } else {
                    // If not specified, check if it's optional.
                    if (!contextMeta.optional) {
                        return false
                    }

                    // If default value provided but no matcher provided, use the default matcher.
                    if (contextMeta.defaultValue != null || contextMeta.nullable) {
                        val matcherOrDefault = matcher ?: contextMeta.defaultMatcher
                        if (matcherOrDefault == null) {
                            throw IllegalStateException(
                                "No matcher found for context value optional $key, no default matcher provided " +
                                        "in comparing ${context.permission} and ${context.record}."
                            )
                        }
                        val result = matcherOrDefault.isMatchedOrNull(contextMeta.defaultValue)
                        if (result == null || !result) {
                            return false
                        }
                    } else {
                        throw IllegalStateException(
                            "Default context value $key is null, but it's not nullable."
                        )
                    }
                }
            }
        }

        return true
    }
}