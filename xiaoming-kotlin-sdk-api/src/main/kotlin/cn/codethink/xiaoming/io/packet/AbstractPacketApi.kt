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

import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.ErrorMessageCause
import cn.codethink.xiaoming.common.LanguageConfiguration
import cn.codethink.xiaoming.common.PACKET_TYPE_REQUEST
import cn.codethink.xiaoming.common.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingProtocolSubject
import cn.codethink.xiaoming.common.buildUnsupportedPacketTypeArguments
import cn.codethink.xiaoming.io.ERROR_UNSUPPORTED_PACKET_TYPE
import cn.codethink.xiaoming.io.data.Packet
import cn.codethink.xiaoming.io.data.ReceiptPacket
import io.github.oshai.kotlinlogging.KLogger

/**
 * The configuration of [PacketApi].
 *
 * @author Chuanwise
 */
interface PacketApiConfiguration {
    val language: LanguageConfiguration
}

/**
 * Used to send and receive packet.
 *
 * @author Chuanwise
 */
interface PacketApi : AutoCloseable {
    val logger: KLogger
    val subject: Subject
    val configuration: PacketApiConfiguration

    suspend fun send(packet: Packet)
    suspend fun receive(packet: Packet)
}

abstract class AbstractPacketApi(
    override val logger: KLogger,
    override val subject: Subject,
    override val configuration: PacketApiConfiguration
) : PacketApi {
    val requestPacketHandler = RequestPacketHandler().apply {
        registerPacketHandler(PACKET_TYPE_REQUEST, XiaomingProtocolSubject, this)
    }

    private val handlers = DefaultStringMapRegistrations<PacketHandler>()

    override suspend fun receive(packet: Packet) {
        val context = PacketContext(this, packet)

        val packetHandlerRegistration = handlers[packet.type]
        if (packetHandlerRegistration == null) {
            val arguments = buildUnsupportedPacketTypeArguments(packet.type, handlers.toMap().keys)
            val message = configuration.language.unsupportedPacketType.format(arguments)

            context.api.send(
                ReceiptPacket(
                    id = randomPacketId(),
                    state = RECEIPT_STATE_FAILED,
                    request = packet.id,
                    cause = ErrorMessageCause(
                        error = ERROR_UNSUPPORTED_PACKET_TYPE,
                        message = message,
                        context = arguments
                    )
                )
            )
            logger.warn {
                "Packet type ${packet.type} is not supported by the protocol, acceptable types are: ${handlers.toMap().keys}."
            }
            return
        }

        packetHandlerRegistration.value.handle(context)
    }

    fun registerPacketHandler(type: String, subject: Subject, handler: PacketHandler) {
        handlers.register(type, DefaultRegistration(handler, subject))
    }

    fun unregisterPacketHandler(subject: Subject) {
        handlers.unregisterBySubject(subject)
    }

    fun unregisterPacketHandler(type: String) {
        handlers.unregisterByKey(type)
    }
}
