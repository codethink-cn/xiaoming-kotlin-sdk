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
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(InternalApi::class)
class LocalPluginManagerTest {
    private val platform = TestLocalPlatform()

    private object TestPluginHandler : PluginHandler {
        val operationStrings = mutableListOf<String>()

        override suspend fun onAllocate(context: PluginAllocateContext) {
            operationStrings.add("allocate")
            println("${context.plugin.signature} allocated")
        }

        override suspend fun onLoad(context: PluginLoadContext) {
            operationStrings.add("load")
            println("${context.plugin.signature} loaded")
        }

        override suspend fun onEnable(context: PluginEnableContext) {
            operationStrings.add("enable")
            println("${context.plugin.signature} enabled")
        }

        override suspend fun onDisable(context: PluginDisableContext) {
            operationStrings.add("disable")
            println("${context.plugin.signature} disabled")
        }

        override suspend fun onUnload(context: PluginUnloadContext) {
            operationStrings.add("unload")
            println("${context.plugin.signature} unloaded")
        }

        override suspend fun onExit(context: PluginExitContext) {
            operationStrings.add("exit")
            println("${context.plugin.signature} exited")
        }
    }

    private val operation = Operation("Just for Test", TestSubjectDescriptor)
    private val configuration = PluginConfiguration(crashOnRemoved = false)
    private val configurationRetainOnConflict = PluginConfiguration(crashOnRemoved = false, retainOnConflict = true)

    private val pluginManager = platform.pluginManager

    @Test
    fun testSimpleSolution(): Unit = runBlocking {
        val mcPlugin = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        val mcChatPlugin = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat100, configuration, TestPluginHandler, operation)
        val mcProPlugin = pluginManager.registerAvailablePlugin(TestPluginConstants.mcPro100, configuration, TestPluginHandler, operation)

        assertFalse(mcPlugin.isLoaded)
        assertFalse(mcChatPlugin.isLoaded)
        assertFalse(mcProPlugin.isLoaded)

        pluginManager.loadPlugins(operation)

        assertFalse(mcPlugin.isLoaded)
        assertTrue(mcChatPlugin.isLoaded)
        assertTrue(mcProPlugin.isLoaded)

        assertEquals(mcProPlugin, pluginManager.getProviderPlugin(mcPlugin.id))
    }

    @Test
    fun testMultiVersionLackDependencySolution(): Unit = runBlocking {
        val mc100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        val mc200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc200, configuration, TestPluginHandler, operation)
        val mcChat100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat100, configuration, TestPluginHandler, operation)

        // mc-chat:2.0.0 需要 im 插件，但是没有安装，所以哪怕它版本再高，也只能用 mc-chat:1.0.0
        val mcChat200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat200, configuration, TestPluginHandler, operation)

        assertFalse(mc100.isLoaded)
        assertFalse(mc200.isLoaded)
        assertFalse(mcChat100.isLoaded)
        assertFalse(mcChat200.isLoaded)

        pluginManager.loadPlugins(operation)

        assertFalse(mc100.isLoaded)
        assertTrue(mc200.isLoaded)
        assertTrue(mcChat100.isLoaded)
        assertFalse(mcChat200.isLoaded)
    }

    @Test
    fun testMultiVersionHighProvisionFirstSolution(): Unit = runBlocking {
        val mc100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        val mc200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc200, configuration, TestPluginHandler, operation)

        // 尽管 mc-pro 比 mc 高级，但是它只能提供 mc:1.0.0 的功能，所以系统会选择 mc:2.0.0
        val mcPro100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcPro100, configuration, TestPluginHandler, operation)
        val mcChat100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat100, configuration, TestPluginHandler, operation)
        val mcChat200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat200, configuration, TestPluginHandler, operation)
        val im100 = pluginManager.registerAvailablePlugin(TestPluginConstants.im100, configuration, TestPluginHandler, operation)
        val imPro100 = pluginManager.registerAvailablePlugin(TestPluginConstants.imPro100, configuration, TestPluginHandler, operation)

        pluginManager.loadPlugins(operation)

        assertFalse(mc100.isLoaded)
        assertTrue(mc200.isLoaded)
        assertFalse(mcPro100.isLoaded)

        assertFalse(mcChat100.isLoaded)
        assertTrue(mcChat200.isLoaded)

        assertFalse(im100.isLoaded)
        assertTrue(imPro100.isLoaded)
    }

    @Test
    fun testIndirectDependencySolution(): Unit = runBlocking {
        val mc100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        val mc200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc200, configuration, TestPluginHandler, operation)
        val mcChat100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat100, configuration, TestPluginHandler, operation)
        val mcChat200 = pluginManager.registerAvailablePlugin(TestPluginConstants.mcChat200, configuration, TestPluginHandler, operation)
        val im100 = pluginManager.registerAvailablePlugin(TestPluginConstants.im100, configuration, TestPluginHandler, operation)
        val imPro100 = pluginManager.registerAvailablePlugin(TestPluginConstants.imPro100, configuration, TestPluginHandler, operation)

        // cmi:1.0.0 需要 mc-chat:2.0.0，而它又需要 im:1.0.0（显然选择 im-pro:1.0.0） + mc:2.0.0
        val cmi100 = pluginManager.registerAvailablePlugin(TestPluginConstants.cmi100, configurationRetainOnConflict, TestPluginHandler, operation)

        pluginManager.loadPlugins(operation)

        assertFalse(mc100.isLoaded)
        assertTrue(mc200.isLoaded)

        assertFalse(mcChat100.isLoaded)
        assertTrue(mcChat200.isLoaded)

        assertFalse(im100.isLoaded)
        assertTrue(imPro100.isLoaded)

        assertTrue(cmi100.isLoaded)

        val cmi200 = pluginManager.registerAvailablePlugin(TestPluginConstants.cmi200, configuration, TestPluginHandler, operation)

        // There is another plugin com.example:cmi with version 1.0.0 is already loaded.
        // Note that multiple plugins with the same ID are not allowed to be loaded at the same time. Please unload the old one first.
        assertThrows<IllegalStateException> {
            cmi200.load(operation)
        }

        cmi100.unload(operation)
        cmi200.load(operation)
    }

    @Test
    fun testRelease(): Unit = runBlocking {
        val mc100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)

        assertFalse(mc100.isAllocated)
        mc100.release(operation)

        assertTrue(mc100.isReleased)
        assertEquals(null, pluginManager.getAvailablePlugin(TestPluginConstants.mc100.signature))
    }

    @Test
    fun testCrashed(): Unit = runBlocking {
        val mc100 = pluginManager.registerAvailablePlugin(TestPluginConstants.mc100, configuration, TestPluginHandler, operation)
        mc100 as AbstractPlugin

        assertFalse(mc100.isAllocated)

        val crashOperation = Operation("Test Crashed!", TestSubjectDescriptor)
        mc100.crash(crashOperation)

        assertTrue(mc100.isCrashed)
        assertEquals(null, pluginManager.getAvailablePlugin(TestPluginConstants.mc100.signature))
    }
}