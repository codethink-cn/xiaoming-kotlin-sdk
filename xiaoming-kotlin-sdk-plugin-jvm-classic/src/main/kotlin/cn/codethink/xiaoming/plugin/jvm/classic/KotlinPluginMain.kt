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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.plugin.id
import cn.codethink.xiaoming.plugin.jvm.classic.util.createPluginLoggerName
import cn.codethink.xiaoming.plugin.jvm.classic.util.orThrowException
import cn.codethink.xiaoming.util.InternalApi
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

@OptIn(InternalApi::class)
open class KotlinPluginMain : AbstractPluginMain(), CoroutineScope {
    private var mutableJob: Job? = null
    private val job: Job get() = mutableJob.orThrowException()

    private var mutableScope: CoroutineScope? = null
    private val scope: CoroutineScope get() = mutableScope.orThrowException()
    override val coroutineContext: CoroutineContext get() = scope.coroutineContext

    private var mutableLogger = null as KLogger?
    val logger: KLogger get() = mutableLogger.orThrowException()

    override fun onAllocate0() {
        val job = SupervisorJob()

        mutableJob = job
        mutableScope = CoroutineScope(job + platform.coroutineContext)

        mutableLogger = KotlinLogging.logger(createPluginLoggerName(plugin.id))
    }

    override fun onExit0() {
        mutableJob?.cancel()
        mutableJob = null

        mutableScope = null
        mutableLogger = null
    }
}