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

@file:JvmName("Sdks")

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.Platform
import java.util.Properties

const val SDK_PROPERTIES_PATH = "xiaoming/sdk.properties"

val SdkProperties = Properties().apply {
    load(Platform::class.java.classLoader.getResourceAsStream(SDK_PROPERTIES_PATH))
}

val SdkName: String
    get() = SdkProperties["name"] as String

val SdkGroup: String
    get() = SdkProperties["group"] as String

val SdkVersionString: String
    get() = SdkProperties["version"] as String

val SdkVersion: Version = SdkVersionString.toVersion()

val SdkProtocolString: String
    get() = SdkProperties["protocol"] as String

val SdkProtocol: Version = SdkProtocolString.toVersion()