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

import cn.codethink.xiaoming.util.Subject

/**
 * 权限处理器，用于处理对某类主体的权限操作。主要是测试和设置操作。
 *
 * 不同类型的主体的权限查询方式可能不同。例如对于某些外部用户，其权限并不和某个固定的权限包绑定。查询其是否具备某一权限时，
 * 可能先查询是否有对应权限包，再查询某种默认权限性质的权限包。然而，对于其他类型的主体，其查询方式又有不同。设置权限时同样
 * 因此存在差异，因此使用本接口屏蔽这种差异。
 *
 * @param T 主体的描述符类型
 * @author Chuanwise
 * @see LocalPermissionManager.testPermission
 */
interface PermissionHandler<T : Subject> {
    fun onTest(context: PermissionTestContext<T>): Boolean?
    fun onSet(context: PermissionSetContext<T>)
}