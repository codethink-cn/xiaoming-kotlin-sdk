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

import cn.codethink.xiaoming.AbstractLocalPlatform
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MapRegistration
import cn.codethink.xiaoming.util.MutableMapRegistration
import cn.codethink.xiaoming.util.MutableMapRegistrationManagerImpl
import cn.codethink.xiaoming.util.Registration
import cn.codethink.xiaoming.util.Subject
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.Tristate
import io.github.oshai.kotlinlogging.KotlinLogging
import java.lang.IllegalArgumentException
import kotlin.reflect.jvm.jvmName

@OptIn(InternalApi::class)
class LocalPermissionManagerImpl(
    override val platform: AbstractLocalPlatform
) : LocalPermissionManager {
    private val logger = KotlinLogging.logger(PermissionManager::class.jvmName)
    override val permissionHandlers = MutableMapRegistrationManagerImpl<String, PermissionHandler<*>>()

    override fun getInheritedBundleIds(bundleId: Id): Set<Id> {
        data class InheritanceTree(
            val son: InheritanceTree?,
            val bundleId: Id
        )

        val treeNodes = ArrayDeque<InheritanceTree>()
        treeNodes.addLast(InheritanceTree(null, bundleId))

        val visitedBundleIds = mutableSetOf(bundleId)
        while (treeNodes.isNotEmpty()) {
            val treeNode = treeNodes.removeFirst()

            platform.data.getPermissionEntriesByPermissionBundleId(treeNode.bundleId)
                .filter { it.matcher is InheritancePermissionMatcher }
                .forEach {
                    val inheritedProfileId = (it.matcher as InheritancePermissionMatcher).inheritedId
                    if (visitedBundleIds.add(inheritedProfileId)) {
                        treeNodes.addLast(InheritanceTree(treeNode, inheritedProfileId))
                    }
                }
        }
        return visitedBundleIds
    }

    override fun isInherited(parentBundleId: Id, childBundleId: Id): Boolean {
        return getInheritedBundleIds(childBundleId).contains(parentBundleId)
    }

    override fun setPermission(
        bundleId: Id,
        matcher: PermissionMatcher,
        constraints: Map<String, PermissionConstraint>,
        operation: Operation
    ) {
        logger.trace {
            buildString {
                when (matcher) {
                    is InheritancePermissionMatcher -> {
                        append("Make bundle $bundleId inherited from bundle ${matcher.inheritedId}")
                        check(!isInherited(bundleId, matcher.inheritedId)) {
                            "Loop inheritance detected: $bundleId -> ... -> ${matcher.inheritedId}!"
                        }
                    }

                    is WildCardPermissionMatcher -> {
                        append("Set bundle $bundleId permission ${matcher.id} to ${matcher.value}")
                    }

                    else -> throw IllegalArgumentException("Unexpected permission matcher type: ${matcher::class}")
                }

                if (constraints.isNotEmpty()) {
                    val evaluatorsToString = constraints.map { (key, value) -> "$key: $value" }.joinToString(", ")
                    append(" with evaluators $evaluatorsToString ")
                } else {
                    append(" without evaluators ")
                }

                append(operation.description)
            }
        }
        platform.data.addPermissionEntry(bundleId, matcher, constraints, operation)
    }

    private fun throwNoSuchPermissionHandlerException(type: String): Nothing {
        throw NoSuchPermissionHandlerExceptionImpl("No permission handler found for descriptor type $type.", type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPermissionHandlerRegistrationOrFail(type: String): Registration<PermissionHandler<Subject>> {
        return (permissionHandlers.getRegistration(type) ?: throwNoSuchPermissionHandlerException(type)) as Registration<PermissionHandler<Subject>>
    }

    override fun testPermission(subject: Subject, permission: Permission, operation: Operation): Boolean? {
        logger.trace { "Test permission ${permission.id} of subject $subject by ${operation.operator} due to: ${operation.cause.description}" }

        val permissionHandler = getPermissionHandlerRegistrationOrFail(subject.descriptor.type)
        val testContext = PermissionTestContextImpl(
            platform = platform,
            manager = this,
            subject = subject,
            permission = permission,
            operation = operation,
            handler = permissionHandler
        )

        return try {
            permissionHandler.value.onTest(testContext)
        } catch (e: Throwable) {
            platform.exceptionManager.handleException(testContext, e)
            null
        }
    }

    override fun testPermission(bundleId: Id, permission: Permission, operation: Operation): Tristate? {
        logger.trace { "Test permission ${permission.id} of bundle $bundleId by ${operation.operator} due to: ${operation.cause.description}" }

        val entries = platform.data.getPermissionEntriesByPermissionBundleId(bundleId)
        if (entries.isEmpty()) {
            logger.trace { "No permission entry found for bundle $bundleId" }
            return null
        }

        for (entry in entries) {
            logger.trace { "Testing permission ${permission.id} of entry ${entry.id}" }

            val matcherContext = PermissionMatcherContextImpl(
                platform = platform,
                manager = this,
                bundleId = bundleId,
                permission = permission,
                operation = operation,
                entry = entry
            )

            val checkResult = try {
                entry.matcher.matches(matcherContext)
            } catch (e: Throwable) {
                platform.exceptionManager.handleException(matcherContext, e)
                continue
            } ?: continue

            var satisfyResult = true
            for ((constraintId, constraint) in entry.constraints) {
                val constraintContext = PermissionConstraintContextImpl(
                    platform = platform,
                    manager = this,
                    bundleId = bundleId,
                    permission = permission,
                    operation = operation,
                    entry = entry,
                    constraintId = constraintId,
                    constraint = constraint
                )

                satisfyResult = try {
                    constraint.satisfies(constraintContext)
                } catch (e: Throwable) {
                    platform.exceptionManager.handleException(constraintContext, e)
                    continue
                }

                if (!satisfyResult) {
                    logger.trace { "Permission constraint $constraintId not satisfied for permission ${permission.id} of entry ${entry.id}" }
                    break
                }
            }

            if (satisfyResult) {
                return checkResult
            }
        }
        return null
    }

    override fun registerPermissionHandler(
        type: String,
        handler: PermissionHandler<*>,
        operation: Operation
    ): MutableMapRegistration<String, PermissionHandler<*>> {
        logger.trace { "Register permission handler $handler for subject descriptor type $type ${operation.description}" }
        return permissionHandlers.register(type, handler, operation)
    }

    override fun unregisterPermissionHandler(type: String, operation: Operation): MapRegistration<String, PermissionHandler<*>>? {
        logger.trace { "Unregister permission handler for subject descriptor type $type ${operation.description}" }
        return permissionHandlers.remove(type)
    }
}