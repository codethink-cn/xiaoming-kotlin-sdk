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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.DefaultSegmentIdMatcher
import cn.codethink.xiaoming.common.Matcher
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.isMatchable
import cn.codethink.xiaoming.common.isMatchedOrNull
import cn.codethink.xiaoming.permission.data.PermissionProfile
import cn.codethink.xiaoming.permission.data.PermissionRecord

data class PermissionComparingContext(
    val api: LocalPermissionServiceApi,
    val profile: PermissionProfile,
    val permission: Permission,
    val record: PermissionRecord,
    val context: Map<String, Any?> = emptyMap(),
    val caller: Subject? = null,
    val cause: Cause? = null
)


interface PermissionComparator {
    fun compare(context: PermissionComparingContext): Boolean
}

interface PermissionComparingCheckingCallbackSupport<T> : Matcher<T> {
    fun onPermissionComparingChecking(context: PermissionComparingContext, value: T): Boolean
}

/**
 * Comparator to match the permission subject, id and contexts.
 *
 * The comparator will first check if given permission node is matched with the
 * record node. For example, record node `a.b.*` matches given `a.b.c`, but record
 * node `a.*` doesn't match `a.b.c` by default. Details see [DefaultSegmentIdMatcher].
 *
 * Then, the comparator will check if the permission context is matched with the
 * record context. If the permission meta is registered, the context will be checked
 * according it. Otherwise, the context will be checked in the most strict way.
 *
 * @author Chuanwise
 * @see DefaultSegmentIdMatcher
 * @see PermissionComparator
 * @see DefaultPermissionMatcher
 * @see PermissionMeta
 * @see PermissionParameterMeta
 */
@Suppress("UNCHECKED_CAST")
object DefaultPermissionComparator : PermissionComparator {
    override fun compare(context: PermissionComparingContext): Boolean {
        val matched = context.record.nodeMatcher.isMatched(context.permission.descriptor.node)
        if (!matched) {
            return false
        }

        // Find permission meta that registered by subject of given permission.
        val meta = context.api.permissionMetas[
            context.permission.descriptor.node, context.permission.descriptor.subject
        ]?.value

        // Match arguments.
        if (meta == null) {
            // Given permission argument keys must equals to record argument matcher keys.
            if (context.record.argumentMatchers.keys != context.permission.arguments.keys) {
                return false
            }

            // Check if the context values are all matched.
            context.permission.arguments.forEach { (key, value) ->
                val matcher = context.record.argumentMatchers[key] as Matcher<Any?>?
                    ?: throw IllegalStateException("No matcher found for key: $key.")

                val result = matcher.isMatchedOrNull(value)
                if (result != null && !result) {
                    return false
                }
            }
        } else {
            // Given permission argument keys contained in meta context.
            if (!meta.parameters.keys.containsAll(context.permission.arguments.keys)) {
                return false
            }

            // Check if the context values are all matched.
            meta.parameters.forEach { (key, contextMeta) ->
                val matcher = context.record.argumentMatchers[key] as Matcher<Any?>?
                val given = context.permission.arguments[key]

                if (given == null && context.permission.arguments.containsKey(key)) {
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

        // Check context.
        if (!context.context.isContextMatched(context) || !context.record.context.isContextMatched(context)) {
            return false
        }

        return true
    }

    private fun Map<String, Any?>.isContextMatched(context: PermissionComparingContext): Boolean {
        forEach { (key, value) ->
            val matcher = context.api.permissionContextMatchers[key]?.value as Matcher<Any?>?
            if (matcher == null) {
                context.api.logger.warn { "No permission context checker found for key: $key." }
                return false
            }

            if (!matcher.isMatchable(value)) {
                context.api.logger.warn { "Context value $key is not matchable: $value." }
                return false
            }

            val result = if (matcher is PermissionComparingCheckingCallbackSupport) {
                matcher.onPermissionComparingChecking(context, value)
            } else {
                matcher.isMatched(value)
            }
            if (!result) {
                return false
            }
        }
        return true
    }
}