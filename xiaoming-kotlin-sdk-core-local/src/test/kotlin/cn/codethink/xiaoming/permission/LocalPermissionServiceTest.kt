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

package cn.codethink.xiaoming.permission

import cn.codethink.xiaoming.common.CurrentProtocolSubject
import cn.codethink.xiaoming.io.LocalPlatformModule
import cn.codethink.xiaoming.io.data.PlatformAnnotationIntrospector
import cn.codethink.xiaoming.io.data.PlatformModule
import cn.codethink.xiaoming.permission.data.DatabaseLocalPlatformData
import cn.codethink.xiaoming.permission.data.LocalPlatformConfiguration
import cn.codethink.xiaoming.permission.data.insertAndGetProfile
import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import java.io.InputStream

class LocalPermissionServiceTest {
    companion object {
        val mapper = jacksonObjectMapper().apply {
            setAnnotationIntrospector(
                AnnotationIntrospector.pair(
                    PlatformAnnotationIntrospector(),
                    JacksonAnnotationIntrospector()
                )
            )
            registerModules(
                PlatformModule(),
                LocalPlatformModule()
            )
        }

        lateinit var configuration: LocalPlatformConfiguration

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            configuration = getResourceFileAsStream("configurations/configurations.json").use {
                mapper.readValue(it)
            }
            if (configuration.data is DatabaseLocalPlatformData) {
                val dataSource = (configuration.data as DatabaseLocalPlatformData).source.toDataSource()
                Database.Companion.connect(dataSource)
            }
        }

        fun getResourceFileAsStream(path: String): InputStream {
            val classLoader = ClassLoader.getSystemClassLoader()
            val stream = classLoader.getResourceAsStream(path)
                ?: if (classLoader.getResourceAsStream(path + ".example") == null) {
                    throw IllegalArgumentException("Resource not found: $path.")
                } else {
                    throw IllegalArgumentException(
                        "Resource not found: $path. Check, modify and rename it from template `$path.example`."
                    )
                }
            return stream
        }
    }

    @Test
    fun testGetPermissionSubject() {
        val profile = configuration.data.permissionProfiles.insertAndGetProfile(CurrentProtocolSubject)
        println(profile)
    }
}