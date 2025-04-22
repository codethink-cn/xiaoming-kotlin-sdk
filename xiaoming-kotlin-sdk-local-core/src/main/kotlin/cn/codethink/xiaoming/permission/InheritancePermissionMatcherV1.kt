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


package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.Tristate
import kotlin.concurrent.getOrSet

class InheritancePermissionMatcherV1(
    override val inheritedId: Id
) : AbstractPermissionMatcher(), InheritancePermissionMatcher {
    companion object {
        private val INHERITANCE_LOOP_CHECKER = ThreadLocal<ArrayDeque<Id>>()
    }

    override val version: String = "1"

    private inline fun <reified T> PermissionMatcherContext.withInheritanceLoopChecking(action: () -> T): T {
        INHERITANCE_LOOP_CHECKER.getOrSet { ArrayDeque() }.apply {
            if (contains(bundleId)) {
                error(
                    "Detected inheritance loop when matching inheritance permission! " +
                            "Permission set path: ${joinToString(" -> ")}."
                )
            }
            addLast(bundleId)
        }

        try {
            return action()
        } finally {
            val stack = INHERITANCE_LOOP_CHECKER.get()
            checkNotNull(stack) {
                "Inheritance stack is null when matching inheritance permission: ${stack.joinToString(" -> ")}."
            }

            if (stack.isEmpty() || (stack.removeLast() != bundleId)) {
                stack.clear()
                error("Inheritance stack is broken when matching inheritance permission: ${stack.joinToString(" -> ")}.")
            }
            if (stack.isEmpty()) {
                INHERITANCE_LOOP_CHECKER.remove()
            }
        }
    }

    override fun matches(context: PermissionMatcherContext): Tristate? {
        return context.withInheritanceLoopChecking {
            context.manager.testPermission(inheritedId, context.permission, context.operation)
        }
    }
}