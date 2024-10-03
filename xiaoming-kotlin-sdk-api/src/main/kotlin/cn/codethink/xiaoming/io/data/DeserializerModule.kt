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

@file:JvmName("Jackson")

package cn.codethink.xiaoming.io.data

import cn.codethink.xiaoming.common.DataDeserializerModifier
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.common.prependOrNull
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.Module
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ServiceLoader

class DeserializerModule(
    private val name: String = "DeserializerModule",
    private val version: Version = Version.unknownVersion(),
    logger: KLogger = KotlinLogging.logger { }
) : Module() {
    override fun getModuleName(): String = name
    override fun version(): Version = version

    val deserializers = PolymorphicDeserializers(logger)
    val deserializerModifier = DataDeserializerModifier

    override fun setupModule(context: SetupContext) {
        context.addDeserializers(deserializers)
        context.addBeanDeserializerModifier(deserializerModifier)
    }
}

val XiaomingJacksonModuleVersion = XiaomingSdkSubject.let {
    val version = it.version
    val snapshotInfo = version.preRelease.orEmpty() + version.build.prependOrNull("+").orEmpty()
    Version(version.major, version.minor, version.patch, snapshotInfo, it.group, it.name)
}

/**
 * Use service provider interface to initialize [PolymorphicDeserializers].
 *
 * @author Chuanwise
 * @see findAndApplyInitializers
 */
interface PolymorphicDeserializerInitializer {
    fun initialize(deserializers: PolymorphicDeserializers, subject: Subject)
}

fun DeserializerModule.findAndApplyInitializers(subject: Subject) = apply {
    ServiceLoader.load(PolymorphicDeserializerInitializer::class.java)
        .forEach { it.initialize(deserializers, subject) }
}