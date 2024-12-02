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

import cn.codethink.xiaoming.util.SubjectDescriptor

/**
 * 权限测试时的上下文。
 *
 * @author Chuanwise
 */
interface PermissionTestContext<S : SubjectDescriptor> {
    val subject: S
}

/**
 * 权限计算器用于计算某个主体是否具备给定权限。
 *
 * 对于某些外部用户，其权限并不和某个固定的权限账号绑定。对其查询可能先查询是否有固定账号，若无，再查询某种
 * 默认权限性质的账号。然而对于其他一些没有默认权限的主体，其查询权限的方式则不同，因此需要特定权限计算器。
 *
 * @param T 主体的描述符类型
 * @author Chuanwise
 */
interface PermissionHandler<S : SubjectDescriptor> {
    suspend fun onTest(context: PermissionCalculatingContext<T>): Boolean?
}