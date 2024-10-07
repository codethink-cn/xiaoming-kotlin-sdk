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

/**
 * Represent a result that may be successful or failed.
 *
 * If success, result is [Succeed]. Otherwise, if error policy is [ErrorPolicy.RETURN_CAUSE]
 *
 * @author Chuanwise
 * @see ErrorPolicy
 */
sealed interface Expected<out T>

sealed interface Succeed<T> : Expected<T> {
    val value: T
}

data class NormalSucceed<T>(override val value: T) : Succeed<T>

data object UnitSucceed : Succeed<Unit> {
    override val value: Unit = Unit
}

sealed interface Failed : Expected<Nothing> {
    val cause: Cause?
}

data class CauseFailed(override val cause: Cause) : Failed

data object NoCauseFailed : Failed {
    override val cause: Cause? = null
}

val Failed.causeOrFail: Cause
    get() = cause ?: throw NoSuchElementException(
        "No failure cause found, make sure the corresponding error policy is `RETURN_CAUSE`."
    )

@Suppress("UNCHECKED_CAST")
inline fun <reified T> success(value: T): Succeed<T> =
    if (value == Unit) (UnitSucceed as Succeed<T>) else NormalSucceed(value)

inline fun <reified T> T.toSuccess() = success(this)

fun success(): Succeed<Unit> = UnitSucceed

fun failure(cause: Cause?): Failed = if (cause == null) NoCauseFailed else CauseFailed(cause)

fun Cause?.toFailure() = failure(this)

inline fun <reified T> Expected<T>.forCause(block: (Cause?) -> Unit) = apply {
    if (this is Failed) {
        block(cause)
    }
}

inline fun <reified T> Expected<T>.forValue(block: (T) -> Unit) = apply {
    if (this is Succeed) {
        block(value)
    }
}

inline fun <reified T> Expected<T>.ifSucceed(block: (Succeed<T>) -> Unit) = apply {
    if (this is Succeed) {
        block(this)
    }
}

inline fun <reified T> Expected<T>.ifFailed(block: (Failed) -> Unit) = apply {
    if (this is Failed) {
        block(this)
    }
}

val Expected<*>.isSucceed: Boolean
    get() = this is Succeed

val Expected<*>.isFailed: Boolean
    get() = this is Failed

val <T> Expected<T>.valueOrFail: T
    get() = if (!isSucceed) error("Expected value, but failed.") else (this as Succeed).value

val <T> Expected<T>.causeOrFail: Cause
    get() = if (!isFailed) error("Expected cause, but succeed.") else (this as Failed).causeOrFail
