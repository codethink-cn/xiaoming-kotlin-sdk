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

package cn.codethink.xiaoming.exception

import cn.codethink.xiaoming.permission.PermissionMatcherContext
import cn.codethink.xiaoming.permission.PermissionConstraintContext
import cn.codethink.xiaoming.permission.PermissionTestContext

/**
 * 异常处理器。
 *
 * @author Chuanwise
 */
interface LocalExceptionManager : ExceptionManager {
    fun handleException(exception: Throwable)

    fun handleException(context: PermissionTestContext<*>, exception: Throwable) = handleException(exception)
    fun handleException(context: PermissionConstraintContext, exception: Throwable) = handleException(exception)
    fun handleException(context: PermissionMatcherContext, exception: Throwable) = handleException(exception)
}