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

import cn.codethink.xiaoming.serialization.CodecResolverImpl
import cn.codethink.xiaoming.serialization.SerializationManagerImpl
import cn.codethink.xiaoming.serialization.findAndApplyInitializers
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test

class JvmClassicPluginMetaTest {
    private val content = """
        meta-version: 1
        id: cn.codethink:example
        name: Example
        version: 0.1.0-SNAPSHOT
        main: cn.codethink.example.ExamplePluginMain
        description: Example JVM classic plugin.
    """.trimIndent()

    @OptIn(InternalApi::class)
    private val operation = Operation("Just for Test", TestSubjectDescriptor)

    private val serializationManager = SerializationManagerImpl(
        codecResolver = CodecResolverImpl().apply {
            findAndApplyInitializers(operation)
        },
        findAndRegisterModules = true
    )

    @Test
    fun testDeserialize() {
        val configuration = serializationManager.yamlFileObjectMapper.readValue<JvmClassicPluginMeta>(content)

        println(configuration)
    }
}