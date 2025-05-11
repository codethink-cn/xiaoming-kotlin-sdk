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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.TestLocalPlatform
import cn.codethink.xiaoming.util.ExperimentalApi
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(InternalApi::class, ExperimentalApi::class)
class LocalPluginManagerTest {
    private val platform = TestLocalPlatform()

    private object TestPluginHandler : PluginHandler {
        val operationStrings = mutableListOf<String>()

        override suspend fun onAllocate(context: PluginAllocateContext) {
            operationStrings.add("allocate")
            println("${context.plugin.name} allocated")
        }

        override suspend fun onLoad(context: PluginLoadContext) {
            operationStrings.add("load")
            println("${context.plugin.name} loaded")
        }

        override suspend fun onEnable(context: PluginEnableContext) {
            operationStrings.add("enable")
            println("${context.plugin.name} enabled")
        }

        override suspend fun onDisable(context: PluginDisableContext) {
            operationStrings.add("disable")
            println("${context.plugin.name} disabled")
        }

        override suspend fun onUnload(context: PluginUnloadContext) {
            operationStrings.add("unload")
            println("${context.plugin.name} unloaded")
        }

        override suspend fun onRelease(context: PluginReleaseContext) {
            operationStrings.add("release")
            println("${context.plugin.name} released")
        }
    }

    @Test
    fun testRegisterPlugin(): Unit = runBlocking {
        val operation = Operation("Just for Test", TestSubjectDescriptor)
        val configuration = PluginConfiguration(crashOnRemoved = false)

        val pluginManager = platform.pluginManager

        val mcPlugin = pluginManager.registerPlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        val mcChatPlugin = pluginManager.registerPlugin(TestPluginConstants.mcChat100, configuration, TestPluginHandler, operation)
        val mcProPlugin = pluginManager.registerPlugin(TestPluginConstants.mcPro100, configuration, TestPluginHandler, operation)

        assertFalse(mcPlugin.isLoaded)
        assertFalse(mcChatPlugin.isLoaded)
        assertFalse(mcProPlugin.isLoaded)

        pluginManager.loadPlugins(operation)

        assertFalse(mcPlugin.isLoaded)
        assertTrue(mcChatPlugin.isLoaded)
        assertTrue(mcProPlugin.isLoaded)

        assertEquals(mcProPlugin, pluginManager.getProviderPlugin(mcPlugin.id))
    }
}