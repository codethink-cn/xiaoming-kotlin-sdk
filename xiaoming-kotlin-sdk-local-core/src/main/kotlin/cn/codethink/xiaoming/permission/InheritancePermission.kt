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
import cn.codethink.xiaoming.common.Field
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.Tristate
import cn.codethink.xiaoming.common.getValue
import cn.codethink.xiaoming.common.tristateOf
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.set
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.Stack
import kotlin.concurrent.getOrSet

const val PERMISSION_COMPARATOR_TYPE_INHERITANCE = "inheritance"

const val INHERITANCE_PERMISSION_COMPARATOR_FIELD_PROFILE_ID = "profile_id"
const val INHERITANCE_PERMISSION_COMPARATOR_FIELD_CONTEXT = "context"
const val INHERITANCE_PERMISSION_COMPARATOR_FIELD_RESULT_MAPPING = "result_mapping"

/**
 * Check if the permission is inherited from another profile.
 *
 * For example, if profile with id `114514` has permission `a.b.c`, and another
 * profile with id `1919810` has `inheritance(profile_id = 114514)`, then `1919810`
 * will inherit all permissions from `114514`, also includes `a.b.c`.
 *
 * @author Chuanwise
 * @see InheritancePermissionComparatorV1
 */
@JsonTypeName(PERMISSION_COMPARATOR_TYPE_INHERITANCE)
interface InheritancePermissionComparator : PermissionComparator {
    val version: String
    val profileId: Id
    val context: Map<String, Any?>
    val resultMapping: Map<Boolean?, Boolean?>
}

const val INHERITANCE_PERMISSION_COMPARATOR_VERSION_1 = "1"

@JsonTypeName(INHERITANCE_PERMISSION_COMPARATOR_VERSION_1)
class InheritancePermissionComparatorV1 : AbstractData, InheritancePermissionComparator {
    companion object {
        @JvmStatic
        private val THREAD_LOCAL_INHERITANCE_STACK = ThreadLocal<Stack<Any>>()
    }

    override val type: String by raw
    override val version: String by raw

    @Field(INHERITANCE_PERMISSION_COMPARATOR_FIELD_PROFILE_ID)
    override val profileId: Id by raw

    override val context: Map<String, Any?> by raw

    @Field(INHERITANCE_PERMISSION_COMPARATOR_FIELD_RESULT_MAPPING)
    override val resultMapping: Map<Boolean?, Boolean?> by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        profileId: Id,
        context: Map<String, Any?> = emptyMap(),
        resultMapping: Map<Boolean?, Boolean?> = emptyMap(),
        raw: Raw = MapRaw()
    ) : super(raw) {
        raw[FIELD_TYPE] = PERMISSION_COMPARATOR_TYPE_INHERITANCE
        raw[FIELD_VERSION] = INHERITANCE_PERMISSION_COMPARATOR_VERSION_1

        raw[INHERITANCE_PERMISSION_COMPARATOR_FIELD_PROFILE_ID] = profileId
        raw[INHERITANCE_PERMISSION_COMPARATOR_FIELD_CONTEXT] = context
        raw[INHERITANCE_PERMISSION_COMPARATOR_FIELD_RESULT_MAPPING] = resultMapping
    }

    override fun compare(context: PermissionComparingContext): Tristate? {
        // Check loop inheritance.
        THREAD_LOCAL_INHERITANCE_STACK.getOrSet { Stack() }.apply {
            if (contains(context.profile.id)) {
                context.internalApi.logger.error {
                    "Detected inheritance loop when matching inheritance permission: ${joinToString(" -> ")}."
                }
                return null
            }
            push(context.profile.id)
        }

        try {
            val profile = context.internalApi.data.getPermissionProfile(profileId)
            if (profile == null) {
                context.internalApi.logger.warn {
                    "Cannot find profile by id $profileId when matching inheritance permission."
                }
                return null
            }

            val inheritedContext = toInheritedContext(context)
            val result = context.permissionServiceApi.hasPermission(
                profile, context.permission, context.cause, inheritedContext
            )
            return tristateOf(resultMapping.getOrDefault(result, result))
        } finally {
            val stack = THREAD_LOCAL_INHERITANCE_STACK.get()
            if (stack.isEmpty() || (stack.pop() != context.profile.id)) {
                stack.clear()
                throw IllegalStateException(
                    "Inheritance stack is broken when matching inheritance permission: ${stack.joinToString(" -> ")}."
                )
            }
        }
    }

    private fun toInheritedContext(context: PermissionComparingContext): Map<String, Any?> {
        if (this.context.isEmpty()) {
            return context.context
        } else {
            return HashMap(context.context).apply {
                putAll(this@InheritancePermissionComparatorV1.context)
            }
        }
    }
}

/**
 * Check if the permission is inherited from itself.
 *
 * @author Chuanwise
 */
object InheritancePermissionSettingChecker : PermissionSettingChecker {
    override fun check(context: PermissionSettingContext) {
        val comparator = context.comparator
        if (comparator !is InheritancePermissionComparator) {
            return
        }

        data class InheritanceTree(
            val parent: InheritanceTree?,
            val profileId: Id
        )

        val treeNodes = ArrayDeque<InheritanceTree>()
        treeNodes.addLast(InheritanceTree(null, comparator.profileId))

        val checkedProfileIds = mutableSetOf<Any>()
        while (treeNodes.isNotEmpty()) {
            val treeNode = treeNodes.removeFirst()
            val profileId = treeNode.profileId

            // Add its sons and check.
            context.internalApi.data.getPermissionRecordsByPermissionProfileId(profileId)
                .filter { it.comparator is InheritancePermissionComparator }
                .forEach {
                    val inheritedProfileId = (it.comparator as InheritancePermissionComparator).profileId
                    if (checkedProfileIds.add(inheritedProfileId)) {
                        if (inheritedProfileId == context.profile.id) {
                            val errorMessage = buildString {
                                append("Cannot inherit from itself, inheritance chain: ")

                                append(context.profile.id)

                                var node: InheritanceTree? = treeNode
                                while (node != null) {
                                    append(" -> ")
                                    append(node.profileId)
                                    node = node.parent
                                }

                                append(".")
                            }
                            throw IllegalArgumentException(errorMessage)
                        }
                        treeNodes.addLast(InheritanceTree(treeNode, inheritedProfileId))
                    }
                }
        }
    }
}