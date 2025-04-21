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

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.MapRegistration
import cn.codethink.xiaoming.util.MapRegistrationManager
import cn.codethink.xiaoming.util.MutableMapRegistration
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.Tristate
import io.github.oshai.kotlinlogging.KLogger

/**
 * 权限管理器，处理有关权限的操作。
 *
 * @author Chuanwise
 * @see PermissionHandler
 */
interface LocalPermissionManager : PermissionManager {
    /**
     * 权限处理器注册表。
     */
    val permissionHandlers: MapRegistrationManager<String, PermissionHandler<*>>

    /**
     * 注册权限处理器，用于处理关于主体的权限操作。
     *
     * @param type 主体类型，即 [SubjectDescriptor.type]
     * @param handler 权限处理器
     * @param operation 动作的跟踪信息
     * @return 注册结果，null 表示注册失败
     */
    fun registerPermissionHandler(
        type: String,
        handler: PermissionHandler<*>,
        operation: Operation
    ): MutableMapRegistration<String, PermissionHandler<*>>

    /**
     * 注销权限处理器。
     *
     * @param type 主体类型，即 [SubjectDescriptor.type]
     * @param operation 动作的跟踪信息
     * @return 注销结果，null 表示注销失败
     */
    fun unregisterPermissionHandler(type: String, operation: Operation): MapRegistration<String, PermissionHandler<*>>?

    /**
     * 设置权限包的权限。
     *
     * @param bundleId 权限包 ID
     * @param matcher 权限匹配器
     * @param constraints 权限约束
     * @param operation 动作的跟踪信息
     */
    fun setPermission(
        bundleId: Id,
        matcher: PermissionMatcher,
        constraints: Map<String, PermissionConstraint>,
        operation: Operation
    )

    /**
     * 测试权限包是否具有权限。
     *
     * @param bundleId 权限包 ID
     * @param permission 权限
     * @param operation 动作的跟踪信息
     * @return 是否具有权限。null 表示默认未定义，[Tristate.NULL] 表示人为重置为未定义
     */
    fun testPermission(
        bundleId: Id,
        permission: Permission,
        operation: Operation
    ): Tristate?

    /**
     * 获取一个权限包所继承的所有其他权限包。
     *
     * @param bundleId 权限包 ID
     * @return 继承的权限包集合
     */
    fun getInheritedBundleIds(bundleId: Id): Set<Id>

    /**
     * 检查两个权限包是否存在继承关系。
     *
     * @param parentBundleId 父权限包
     * @param childBundleId 子权限包
     * @return 是否存在继承关系。同一个权限包不存在继承关系。
     */
    fun isInherited(parentBundleId: Id, childBundleId: Id): Boolean
}