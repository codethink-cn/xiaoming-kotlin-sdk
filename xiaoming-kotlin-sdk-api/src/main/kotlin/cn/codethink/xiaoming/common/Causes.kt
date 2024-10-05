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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Element used to store the cause in coroutine context.
 *
 * Run code in block `launchBy(cause, subject) { ... }` to set the cause and subject.
 *
 * @author Chuanwise
 * @see currentCoroutineCause
 * @see currentCoroutineCauseOrFail
 * @see launchBy
 */
data class CauseSubjectPairElement(
    val cause: Cause
) : AbstractCoroutineContextElement(CauseSubjectPairElement) {
    companion object Key : CoroutineContext.Key<CauseSubjectPairElement>
}

fun CoroutineScope.launchBy(
    cause: Cause, block: suspend CoroutineScope.() -> Unit
) = launch(CauseSubjectPairElement(cause), block = block)

suspend fun currentCoroutineCause(): Cause? {
    return currentCoroutineContext()[CauseSubjectPairElement]?.cause
}

suspend fun currentCoroutineCauseOrFail(): Cause = currentCoroutineCause()
    ?: throw NoSuchElementException(
        "No default cause set in current coroutine! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `launchBy(cause) { ... }` please. "
    )

/**
 * Element used to store the cause in thread local.
 *
 * @author Chuanwise
 * @see currentThreadCause
 * @see currentThreadCauseOrFail
 * @see runBy
 * @see runInlineBy
 */
val threadLocalCause: ThreadLocal<Cause> = ThreadLocal()

fun currentThreadCause(): Cause? = threadLocalCause.get()

fun currentThreadCauseOrFail() = currentThreadCause()
    ?: throw NoSuchElementException(
        "No default cause set in current thread! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `runBy(cause) { ... }` or `runInlineBy(cause) { ... }` please. "
    )

@JavaFriendlyApi
fun runBy(cause: Cause, block: () -> Unit) {
    threadLocalCause.set(cause)
    try {
        block()
    } finally {
        threadLocalCause.remove()
    }
}

inline fun <reified T> runInlineBy(
    cause: Cause, subject: SubjectDescriptor, crossinline block: () -> T
): T {
    threadLocalCause.set(cause)
    try {
        return block()
    } finally {
        threadLocalCause.remove()
    }
}
