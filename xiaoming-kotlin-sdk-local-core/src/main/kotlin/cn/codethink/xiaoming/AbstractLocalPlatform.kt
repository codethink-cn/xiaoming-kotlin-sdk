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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.data.PlatformData
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.coroutines.CoroutineContext

@InternalApi
abstract class AbstractLocalPlatform(
    configuration: LocalPlatformConfiguration
) : LocalPlatform {
    private val job = SupervisorJob(configuration.parentJob)
    private val scope = CoroutineScope(configuration.parentCoroutineContext + job)
    override val coroutineContext: CoroutineContext = scope.coroutineContext

    private val lock = ReentrantReadWriteLock()
    private var stateNoLock: PlatformState = PlatformState.ALLOCATED
    override val state: PlatformState get() = lock.read { stateNoLock }

    val data: PlatformData = configuration.data
    val logger: KLogger = configuration.logger

    override val descriptor = configuration.descriptor

    private inline fun withStartingState(action: () -> Unit) {
        lock.write {
            stateNoLock = when (stateNoLock) {
                PlatformState.ALLOCATED -> PlatformState.STARTING
                PlatformState.STARTING -> error("Concurrent start of platform is not allowed.")
                PlatformState.STARTED -> error("Platform is already started.")
                PlatformState.STARTING_ERRORED -> error("Platform is starting, but an error occurred.")
                else -> error("Unexpected state while starting: $stateNoLock.")
            }
        }

        try {
            action()

            lock.write {
                stateNoLock = when (stateNoLock) {
                    PlatformState.STARTING -> PlatformState.STARTED
                    PlatformState.STARTING_ERRORED -> PlatformState.STARTING_ERRORED

                    PlatformState.STARTED -> error("Platform is already started.")
                    else -> error("Unexpected state after starting: $stateNoLock.")
                }
            }
        } catch (e: Exception) {
            lock.write {
                stateNoLock = when (stateNoLock) {
                    PlatformState.STARTING,
                    PlatformState.STARTING_ERRORED -> PlatformState.STARTING_ERRORED

                    PlatformState.STARTED -> error("Platform is already started.")
                    else -> error("Unexpected state while trying to set staring error: $stateNoLock.")
                }
            }
            throw e
        }
    }

    final override fun start(operation: Operation) = withStartingState { start0(operation) }

    protected abstract fun start0(operation: Operation)
}