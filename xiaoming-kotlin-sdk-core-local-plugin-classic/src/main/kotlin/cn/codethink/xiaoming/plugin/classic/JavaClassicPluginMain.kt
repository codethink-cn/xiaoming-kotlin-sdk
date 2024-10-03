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

package cn.codethink.xiaoming.plugin.classic

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.Subject

interface JavaClassicPluginMain {
    @Throws(Exception::class)
    fun onLoad(context: ClassicPluginContext)

    @Throws(Exception::class)
    fun onEnable(context: ClassicPluginContext)
}

class JavaClassicPluginMainCaller(
    private val main: JavaClassicPluginMain
) : ClassicPluginMainCaller {
    override fun onLoad(plugin: ClassicPlugin, cause: Cause, subject: Subject) {
        TODO("Not yet implemented")
    }

    override fun onEnable(plugin: ClassicPlugin, cause: Cause, subject: Subject) {
        TODO("Not yet implemented")
    }

    override fun onDisable(plugin: ClassicPlugin, cause: Cause, subject: Subject) {
        TODO("Not yet implemented")
    }
}