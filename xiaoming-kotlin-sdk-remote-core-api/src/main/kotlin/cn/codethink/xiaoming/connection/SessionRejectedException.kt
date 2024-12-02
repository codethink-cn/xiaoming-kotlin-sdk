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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.util.InternalImplementedApi

/**
 * 连接被拒绝异常。
 *
 * @author Chuanwise
 * @see SessionContext
 * @see SessionForwardRejectedException
 * @see SessionBackwardRejectedException
 */
@InternalImplementedApi
sealed class SessionRejectedException(message: String) : RuntimeException(message)

/**
 * 该异常可能在 [Connection.start] 和 [SessionBackwardStartContext.accept] 两处抛出。
 *
 * 1. 在 [Connection.start] 中，表示己方向对方建立连接，连接被对方拒绝。
 * 2. 在 [SessionBackwardStartContext.accept] 中，表示对方向己方建立连接，己方同意，但对方拒绝。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
abstract class SessionForwardRejectedException(message: String) : SessionRejectedException(message)

/**
 * 该异常可能在 [Connection.start] 里抛出，表示己方向对方建立连接，连接被己方拒绝。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
abstract class SessionBackwardRejectedException(message: String) : SessionRejectedException(message)