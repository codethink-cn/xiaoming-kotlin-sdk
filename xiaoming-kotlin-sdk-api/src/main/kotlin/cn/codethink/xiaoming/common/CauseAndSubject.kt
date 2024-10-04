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
 * @see currentCoroutineSubject
 * @see currentCoroutineSubjectOrFail
 */
data class CauseSubjectPairElement(
    val cause: Cause,
    val subject: SubjectDescriptor
) : AbstractCoroutineContextElement(CauseSubjectPairElement) {
    companion object Key : CoroutineContext.Key<CauseSubjectPairElement>
}

fun CoroutineScope.launchBy(
    cause: Cause, subject: SubjectDescriptor, block: suspend CoroutineScope.() -> Unit
) = launch(CauseSubjectPairElement(cause, subject), block = block)

suspend fun currentCoroutineCause(): Cause? {
    return currentCoroutineContext()[CauseSubjectPairElement]?.cause
}

suspend fun currentCoroutineCauseOrFail(): Cause = currentCoroutineCause()
    ?: throw NoSuchElementException(
        "No default cause set in current coroutine! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `launchBy(cause, subject) { ... }` please. "
    )

suspend fun currentCoroutineSubject(): SubjectDescriptor? {
    return currentCoroutineContext()[CauseSubjectPairElement]?.subject
}

suspend fun currentCoroutineSubjectOrFail(): SubjectDescriptor = currentCoroutineSubject()
    ?: throw NoSuchElementException(
        "No default subject set in current coroutine! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `launchBy(cause, subject) { ... }` please. "
    )

data class CauseSubjectPair(
    val cause: Cause,
    val subject: SubjectDescriptor
)

/**
 * Element used to store the cause in thread local.
 *
 * @author Chuanwise
 * @see currentThreadCause
 * @see currentThreadCauseOrFail
 * @see currentThreadSubject
 * @see currentThreadSubjectOrFail
 * @see runBy
 * @see runInlineBy
 */
val threadLocalCause: ThreadLocal<CauseSubjectPair> = ThreadLocal()

fun currentThreadCauseSubjectPair() = threadLocalCause.get()

fun currentThreadCauseSubjectPairOrFail() = currentThreadCauseSubjectPair()
    ?: throw NoSuchElementException(
        "No default cause set in current thread! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `runBy(cause) { ... }` or `runInlineBy(cause) { ... }` please. "
    )

fun currentThreadCause(): Cause? = threadLocalCause.get()?.cause

fun currentThreadCauseOrFail() = currentThreadCause()
    ?: throw NoSuchElementException(
        "No default cause set in current thread! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `runBy(cause) { ... }` or `runInlineBy(cause) { ... }` please. "
    )

fun currentThreadSubject(): SubjectDescriptor? = threadLocalCause.get()?.subject

fun currentThreadSubjectOrFail() = currentThreadSubject()
    ?: throw NoSuchElementException(
        "No default subject set in current thread! " +
                "Provide it explicitly by passing argument " +
                "or run codes in block `runBy(cause) { ... }` or `runInlineBy(cause) { ... }` please. "
    )

fun runBy(cause: Cause, subject: SubjectDescriptor, block: () -> Unit) {
    threadLocalCause.set(CauseSubjectPair(cause, subject))
    try {
        block()
    } finally {
        threadLocalCause.remove()
    }
}

inline fun <reified T> runInlineBy(
    cause: Cause, subject: SubjectDescriptor, crossinline block: () -> T
): T {
    threadLocalCause.set(CauseSubjectPair(cause, subject))
    try {
        return block()
    } finally {
        threadLocalCause.remove()
    }
}

fun providedOrFromCurrentThread(cause: Cause?, subject: SubjectDescriptor?): CauseSubjectPair {
    if (cause == null && subject == null) {
        return currentThreadCauseSubjectPairOrFail()
    }
    return CauseSubjectPair(
        cause ?: currentThreadCauseOrFail(),
        subject ?: currentThreadSubjectOrFail()
    )
}