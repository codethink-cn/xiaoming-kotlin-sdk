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
import cn.codethink.xiaoming.event.LocalEventManager
import cn.codethink.xiaoming.event.LocalEventManagerImpl
import cn.codethink.xiaoming.exception.LocalExceptionManager
import cn.codethink.xiaoming.library.LibraryManager
import cn.codethink.xiaoming.permission.LocalPermissionManager
import cn.codethink.xiaoming.permission.PermissionBundle
import cn.codethink.xiaoming.permission.PermissionConstraint
import cn.codethink.xiaoming.permission.PermissionEntry
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.plugin.LocalPluginManager
import cn.codethink.xiaoming.plugin.LocalPluginManagerImpl
import cn.codethink.xiaoming.serialization.CodecResolverImpl
import cn.codethink.xiaoming.serialization.SerializationManager
import cn.codethink.xiaoming.serialization.SerializationManagerImpl
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.SubjectDescriptor
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

object TestPlatformSubjectDescriptor : SubjectDescriptor {
    override val type: String = "test_platform"
}

object TestPlatformData : PlatformData {
    override fun getSubjectById(id: Id): SubjectDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getSubjectId(subject: SubjectDescriptor): Id? {
        TODO("Not yet implemented")
    }

    override fun getOrInsertSubjectId(subject: SubjectDescriptor): Id {
        TODO("Not yet implemented")
    }

    override fun getPermissionBundles(): List<PermissionBundle> {
        TODO("Not yet implemented")
    }

    override fun getPermissionBundles(subjectId: Id): List<PermissionBundle> {
        TODO("Not yet implemented")
    }

    override fun getPermissionBundleById(id: Id): PermissionBundle? {
        TODO("Not yet implemented")
    }

    override fun insertPermissionBundle(subjectId: Id): Id {
        TODO("Not yet implemented")
    }

    override fun getPermissionEntriesByPermissionBundleId(id: Id, reverse: Boolean): List<PermissionEntry> {
        TODO("Not yet implemented")
    }

    override fun removePermissionEntryById(entryId: Id): Int {
        TODO("Not yet implemented")
    }

    override fun addPermissionEntry(bundleId: Id, matcher: PermissionMatcher, constraints: Map<String, PermissionConstraint>, operation: Operation): Id {
        TODO("Not yet implemented")
    }

}

class TestLocalPlatformConfiguration(
    override val logger: KLogger = KotlinLogging.logger("TestPlatform"),
    override val descriptor: SubjectDescriptor = TestPlatformSubjectDescriptor,
    override val parentJob: Job? = null,
    override val parentCoroutineContext: CoroutineContext = Dispatchers.IO,
    override val serializationManager: SerializationManager = SerializationManagerImpl(
        codecResolver = CodecResolverImpl(),
        findAndRegisterModules = true
    ),
    override val data: PlatformData = TestPlatformData
) : LocalPlatformConfiguration

@OptIn(InternalApi::class)
class TestLocalPlatform(
    configuration: LocalPlatformConfiguration = TestLocalPlatformConfiguration()
) : AbstractLocalPlatform(configuration) {
    override val serializationManager: SerializationManager = configuration.serializationManager
    override val libraryManager: LibraryManager get() = TODO()
    override val eventManager: LocalEventManager = LocalEventManagerImpl(this)
    override val permissionManager: LocalPermissionManager get() = TODO()
    override val exceptionManager: LocalExceptionManager get() = TODO()
    override val pluginManager: LocalPluginManager = LocalPluginManagerImpl(this)

    override suspend fun doStart(operation: Operation) {
        logger.info { "Starting test platform..." }
    }

    override suspend fun doStop(operation: Operation) {
        TODO("Not yet implemented")
    }
}