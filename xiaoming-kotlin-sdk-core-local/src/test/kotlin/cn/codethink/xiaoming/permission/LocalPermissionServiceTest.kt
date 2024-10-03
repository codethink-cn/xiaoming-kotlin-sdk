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

import cn.codethink.xiaoming.DefaultLocalPlatformApi
import cn.codethink.xiaoming.DefaultLocalPlatformConfiguration
import cn.codethink.xiaoming.TEST_CAUSE
import cn.codethink.xiaoming.TEST_SUBJECT
import cn.codethink.xiaoming.common.PluginSubject
import cn.codethink.xiaoming.common.getTestResourceAsStream
import cn.codethink.xiaoming.common.segmentIdOf
import cn.codethink.xiaoming.common.toLiteralMatcher
import cn.codethink.xiaoming.data.LocalPlatformDataConfiguration
import cn.codethink.xiaoming.data.insertAndGetPermissionProfile
import cn.codethink.xiaoming.io.DefaultProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.data.DeserializerModule
import cn.codethink.xiaoming.io.data.XiaomingJacksonModuleVersion
import cn.codethink.xiaoming.io.data.findAndApplyInitializers
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Locale

class LocalPermissionServiceTest {
    private val logger = KotlinLogging.logger { }

    private val deserializerModule = DeserializerModule(
        version = XiaomingJacksonModuleVersion,
        logger = logger
    ).apply {
        findAndApplyInitializers(javaClass.classLoader, TEST_SUBJECT)
    }

    private val dataObjectMapper = jacksonObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        findAndRegisterModules()
        registerModule(deserializerModule)
    }

    private val fileObjectMapper = YAMLMapper.builder()
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
        .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
        .addModule(deserializerModule)
        .findAndAddModules()
        .build()

    private val platformApi = DefaultLocalPlatformApi(
        configuration = DefaultLocalPlatformConfiguration(
            subject = TEST_SUBJECT,
            language = getTestResourceAsStream("xiaoming/languages/${Locale.getDefault()}/protocol.yml").use {
                fileObjectMapper.readValue<DefaultProtocolLanguageConfiguration>(it)
            },
            dataObjectMapper = dataObjectMapper,
            deserializerModule = deserializerModule,
            data = getTestResourceAsStream("xiaoming/data.yml").use {
                fileObjectMapper.readValue<LocalPlatformDataConfiguration>(it)
            }
        ),
    ).apply {
        start(TEST_CAUSE, TEST_SUBJECT)
    }
    private val api = platformApi.internalApi

    private val subject = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo"))
    private val subjectMatcher = subject.toLiteralMatcher()

    private val profile = api.data.insertAndGetPermissionProfile(subject)

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
            comparator = DefaultPermissionComparatorV1(
                subjectMatcher = subjectMatcher,
                nodeMatcher = segmentIdOf("a.b.c").toLiteralMatcher(),
                value = false
            )
        )
        assertEquals(null, api.permissionServiceApi.hasPermission(profile, permission))

        // Set the permission to false and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            comparator = DefaultPermissionComparatorV1(
                subjectMatcher = subjectMatcher,
                nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
                value = false
            )
        )
        assertEquals(false, api.permissionServiceApi.hasPermission(profile, permission))

        // Set to true and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            comparator = DefaultPermissionComparatorV1(
                subjectMatcher = subjectMatcher,
                nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
                value = true
            )
        )
        assertEquals(true, api.permissionServiceApi.hasPermission(profile, permission))

        // Unset and check.
        api.permissionServiceApi.setPermission(
            profile = profile,
            comparator = DefaultPermissionComparatorV1(
                subjectMatcher = subjectMatcher,
                nodeMatcher = segmentIdOf("a.b.c.d").toLiteralMatcher(),
                value = null
            )
        )
        assertEquals(null, api.permissionServiceApi.hasPermission(profile, permission))
    }

    @Test
    fun testInheritancePermission() {
        val subjectA = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo.a"))
        val subjectAMatcher = subjectA.toLiteralMatcher()
        val subjectAProfile = api.data.insertAndGetPermissionProfile(subjectA)

        val subjectB = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo.b"))
        val subjectBMatcher = subjectB.toLiteralMatcher()
        val subjectBProfile = api.data.insertAndGetPermissionProfile(subjectB)

        val subjectC = PluginSubject(segmentIdOf("cn.codethink.xiaoming.demo.c"))
        val subjectCMatcher = subjectB.toLiteralMatcher()
        val subjectCProfile = api.data.insertAndGetPermissionProfile(subjectB)

        val permissionASegmentId = segmentIdOf("a.b")
        val permissionA = Permission(
            descriptor = PermissionDescriptor(
                subject = subjectA,
                node = permissionASegmentId
            )
        )
        val permissionAComparatorTrue = DefaultPermissionComparatorV1(
            subjectMatcher = subjectAMatcher,
            nodeMatcher = permissionASegmentId.toLiteralMatcher(),
            value = true
        )

        // A, B, C are empty.
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectAProfile, permissionA))
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectBProfile, permissionA))
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectCProfile, permissionA))

        // A:
        // 1. a.b: true

        // B, C: empty
        api.permissionServiceApi.setPermission(subjectAProfile, permissionAComparatorTrue)
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectAProfile, permissionA))
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectBProfile, permissionA))
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectCProfile, permissionA))

        // A:
        // 1. a.b: true

        // B:
        // 1. inheritance(profile_id = A)

        // C: empty
        api.permissionServiceApi.setPermission(subjectBProfile, InheritancePermissionComparatorV1(subjectAProfile.id))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectAProfile, permissionA))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectBProfile, permissionA))
        assertEquals(null, api.permissionServiceApi.hasPermission(subjectCProfile, permissionA))

        // A:
        // 1. a.b: true

        // B:
        // 1. inheritance(profile_id = A)

        // C:
        // 1. inheritance(profile_id = A)
        api.permissionServiceApi.setPermission(subjectCProfile, InheritancePermissionComparatorV1(subjectAProfile.id))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectAProfile, permissionA))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectBProfile, permissionA))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectCProfile, permissionA))

        // A:
        // 1. a.b: true

        // B:
        // 1. inheritance(profile_id = A)

        // C:
        // 1. inheritance(profile_id = A)
        // 2. inheritance(profile_id = B)
        api.permissionServiceApi.setPermission(subjectCProfile, InheritancePermissionComparatorV1(subjectBProfile.id))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectAProfile, permissionA))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectBProfile, permissionA))
        assertEquals(true, api.permissionServiceApi.hasPermission(subjectCProfile, permissionA))

        // Loop inheritance:
        // C -> B -> C
        assertThrows<IllegalArgumentException> {
            api.permissionServiceApi.setPermission(
                subjectBProfile,
                InheritancePermissionComparatorV1(subjectCProfile.id)
            )
        }
    }
}