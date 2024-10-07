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

package cn.codethink.xiaoming.io.action

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.common.RECEIPT_STATE_INTERRUPTED
import cn.codethink.xiaoming.common.RECEIPT_STATE_SUCCEED

/**
 * Result of handling a request.
 *
 * @author Chuanwise
 * @see RequestHandler
 */
sealed interface RequestResult<R>

/**
 * @see RECEIPT_STATE_SUCCEED
 * @author Chuanwise
 */
interface SucceedRequestResult<R> : RequestResult<R> {
    val data: R
}

@JvmInline
value class DefaultSuccessRequestResult<R>(
    override val data: R
) : SucceedRequestResult<R>

/**
 * @see RECEIPT_STATE_FAILED
 * @author Chuanwise
 */
interface FailedRequestResult<R> : RequestResult<R> {
    val cause: Cause
}

@JvmInline
value class DefaultFailedRequestResult<R>(
    override val cause: Cause
) : FailedRequestResult<R>

/**
 * @see RECEIPT_STATE_INTERRUPTED
 * @author Chuanwise
 */
interface InterruptedRequestResult<R> : RequestResult<R> {
    val cause: Cause
}

@JvmInline
value class DefaultInterruptedRequestResult<R>(
    override val cause: Cause
) : InterruptedRequestResult<R>
