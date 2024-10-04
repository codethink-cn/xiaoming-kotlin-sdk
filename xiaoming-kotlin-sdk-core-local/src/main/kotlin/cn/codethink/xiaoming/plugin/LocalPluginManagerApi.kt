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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.providedOrFromCurrentThread
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class LocalPluginManagerApi(
    val internalApi: LocalPlatformInternalApi
) {
    val lock = ReentrantReadWriteLock()

    @InternalApi
    internal fun start(cause: Cause, subject: SubjectDescriptor) {

    }

    fun enablePlugin(plugin: Plugin, cause: Cause, subject: SubjectDescriptor) = enablePlugins(
        listOf(plugin), cause, subject
    )

    fun enablePlugins(plugins: Iterable<Plugin>, cause: Cause, subject: SubjectDescriptor): Unit = lock.write {
        val (causeOrDefault, subjectOrDefault) = providedOrFromCurrentThread(cause, subject)
        val pluginsById = plugins.associateBy { it.subject.id }

        TODO()
    }
}