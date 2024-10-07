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

package cn.codethink.xiaoming.common

import io.github.oshai.kotlinlogging.KLogger

/**
 * Control platform how to handle expected errors.
 *
 * @author Chuanwise
 */
enum class ErrorPolicy {
    THROW_EXCEPTION,
    LOG_ERROR,
    RETURN_CAUSE
}

@InternalApi
fun KLogger.expectedError(
    cause: Cause,
    descriptor: SubjectDescriptor,
    policy: ErrorPolicy,
    message: () -> String
): Cause? {
    when (policy) {
        ErrorPolicy.THROW_EXCEPTION -> throw IllegalStateException(message())
        ErrorPolicy.LOG_ERROR -> error(message).also { return null }
        ErrorPolicy.RETURN_CAUSE -> return TextCause(message(), descriptor, cause)
    }
}

@InternalApi
fun KLogger.expectedError(
    policy: ErrorPolicy,
    cause: () -> StandardTextCause
): Cause? {
    when (policy) {
        ErrorPolicy.THROW_EXCEPTION -> throw IllegalStateException(cause().text)
        ErrorPolicy.LOG_ERROR -> error { cause().text }.also { return null }
        ErrorPolicy.RETURN_CAUSE -> return cause()
    }
}
