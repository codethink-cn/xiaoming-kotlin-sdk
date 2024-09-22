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

package cn.codethink.xiaoming.io.packet

import cn.codethink.xiaoming.common.AbstractData
import cn.codethink.xiaoming.common.Template
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue

/**
 * Protocol language configuration.
 *
 * @author Chuanwise
 */
class ProtocolLanguageConfiguration(
    raw: Raw
) : AbstractData(raw) {
    val unsupportedPacketType: Template by raw
    val unsupportedRequestMode: Template by raw
    val unsupportedRequestAction: Template by raw
    val internalActionHandlerError: Template by raw
    val actionHandlerTimeout: Template by raw
}

fun buildUnsupportedPacketTypeArguments(
    packetType: String,
    supportedPacketTypes: Set<String>
): Map<String, Any?> = mapOf(
    "packetType" to packetType,
    "supportedPacketTypes" to supportedPacketTypes.joinToString(", ")
)

fun buildUnsupportedRequestModeArguments(
    requestMode: String,
    supportedRequestModes: Set<String>
): Map<String, Any?> = mapOf(
    "requestMode" to requestMode,
    "supportedRequestModes" to supportedRequestModes.joinToString(", ")
)

fun buildUnsupportedRequestActionArguments(requestAction: String): Map<String, Any?> = mapOf(
    "requestAction" to requestAction
)

fun buildActionHandlerTimeoutArguments(timeout: Long): Map<String, Any?> = mapOf(
    "timeout" to timeout
)