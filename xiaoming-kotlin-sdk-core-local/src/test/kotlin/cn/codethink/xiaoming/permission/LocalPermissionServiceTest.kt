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

import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.segmentIdOf
import cn.codethink.xiaoming.common.toLiteralMatcher
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.configuration.LocalPlatformInternalConfiguration
import cn.codethink.xiaoming.io.data.PlatformAnnotationIntrospector
import cn.codethink.xiaoming.permission.data.getOrInsertProfile
import com.fasterxml.jackson.databind.AnnotationIntrospector
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class LocalPermissionServiceTest {
    companion object {
        val logger = KotlinLogging.logger { }
        val mapper = jacksonObjectMapper().apply {
            setAnnotationIntrospector(
                AnnotationIntrospector.pair(
                    PlatformAnnotationIntrospector(),
                    JacksonAnnotationIntrospector()
                )
            )
            findAndRegisterModules()
        }

        val internalConfiguration = LocalPlatformInternalConfiguration(
            workingDirectoryFile = File("platform")
        )
        val api = LocalPlatformInternalApi(internalConfiguration, logger).apply {
            start(TextCause("Run test programs"), XiaomingSdkSubject)
        }

        val subject = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo"))
        val subjectMatcher = subject.toLiteralMatcher()

        val profile = api.data.permissionProfileData.getOrInsertProfile(subject)
    }

    @Test
    fun testSetSimplePermission() {
        val permission = Permission(
            descriptor = PermissionDescriptor(
                subject = subject,
                node = segmentIdOf("a.b.c.d")
            )
        )

        // First the permission is unset.
        assertEquals(null, api.permissionServiceApi.hasPermission(profile, permission))

        // Set the permission to false and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            subjectMatcher = subjectMatcher,
            nodeMatcher = segmentIdOf("a.b.c").toLiteralMatcher(),
            false
        )
        assertEquals(null, api.permissionServiceApi.hasPermission(profile, permission))

        // Set the permission to false and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            subjectMatcher = subjectMatcher,
            nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
            false
        )
        assertEquals(false, api.permissionServiceApi.hasPermission(profile, permission))

        // Set to true and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            subjectMatcher = subjectMatcher,
            nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
            true
        )
        assertEquals(true, api.permissionServiceApi.hasPermission(profile, permission))

        // Unset and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            subjectMatcher = subjectMatcher,
            nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
            null
        )
        assertEquals(null, api.permissionServiceApi.hasPermission(profile, permission))
    }
}