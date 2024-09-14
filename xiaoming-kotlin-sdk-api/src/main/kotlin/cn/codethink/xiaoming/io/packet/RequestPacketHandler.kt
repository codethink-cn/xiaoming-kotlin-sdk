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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.CurrentProtocolSubject
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.ErrorMessageCause
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.PACKET_FIELD_CAUSE
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_DATA
import cn.codethink.xiaoming.common.RECEIPT_PACKET_FIELD_STATE
import cn.codethink.xiaoming.common.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.common.RECEIPT_STATE_INTERRUPTED
import cn.codethink.xiaoming.common.RECEIPT_STATE_SUCCEED
import cn.codethink.xiaoming.common.RECEIPT_STATE_UNDEFINED
import cn.codethink.xiaoming.common.REQUEST_MODE_ASYNC
import cn.codethink.xiaoming.common.REQUEST_MODE_ASYNC_RESULT
import cn.codethink.xiaoming.common.REQUEST_MODE_SYNC
import cn.codethink.xiaoming.common.REQUEST_PACKET_FIELD_ARGUMENT
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.common.currentTimeSeconds
import cn.codethink.xiaoming.io.ACTION_HANDLER_TIMEOUT
import cn.codethink.xiaoming.io.INTERNAL_ACTION_HANDLER_ERROR
import cn.codethink.xiaoming.io.UNSUPPORTED_REQUEST_MODE
import cn.codethink.xiaoming.io.action.Action
import cn.codethink.xiaoming.io.data.ReceiptPacket
import cn.codethink.xiaoming.io.data.RequestPacket
import cn.codethink.xiaoming.io.data.set
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import java.time.Duration
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * The receipt of a request action.
 *
 * @author Chuanwise
 * @see SucceedRequestActionReceipt
 * @see FailedRequestActionReceipt
 */
sealed interface RequestActionReceipt<R> {
    val succeed: Boolean
}

data class SucceedRequestActionReceipt<R>(
    val data: R
) : RequestActionReceipt<R> {
    override val succeed: Boolean
        get() = true
}

data class FailedRequestActionReceipt<R>(
    val cause: Cause
) : RequestActionReceipt<R> {
    override val succeed: Boolean
        get() = false
}

class RequestActionContext(
    api: PacketApi,
    packet: RequestPacket,
    val receipt: ReceiptPacket
) : PacketContext(api, packet)

interface RequestActionHandler<P, R> {
    suspend fun handle(context: RequestActionContext, argument: P): RequestActionReceipt<R>
}

/**
 * Handle the [RequestPacket]s.
 *
 * @author Chuanwise
 */
class RequestPacketHandler : PacketHandler {
    inner class SyncRequestPacketHandler : PacketHandler {
        override suspend fun handle(context: PacketContext) {
            TODO("Not yet implemented")
        }
    }

    inner class AsyncRequestPacketHandler : PacketHandler {
        override suspend fun handle(context: PacketContext) {
            TODO("Not yet implemented")
        }
    }

    inner class AsyncResultRequestPacketHandler : PacketHandler {
        override suspend fun handle(context: PacketContext) {
            TODO("Not yet implemented")
        }
    }

    val requestModeHandlers = MapRegistrations<String, PacketHandler, DefaultRegistration<PacketHandler>>().apply {
        register(REQUEST_MODE_SYNC, DefaultRegistration(SyncRequestPacketHandler(), CurrentProtocolSubject))
        register(REQUEST_MODE_ASYNC, DefaultRegistration(AsyncRequestPacketHandler(), CurrentProtocolSubject))
        register(
            REQUEST_MODE_ASYNC_RESULT,
            DefaultRegistration(AsyncResultRequestPacketHandler(), CurrentProtocolSubject)
        )
    }

    class Metrics {
        val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

        var handleCount: Int = 0
        var succeedCount: Int = 0
        var failedCount: Int = 0
        var timeoutCount: Int = 0
        var durationMillis: Long = 0
    }

    class PacketActionHandlerRegistration<P, R>(
        val action: Action<P, R>,
        override val value: RequestActionHandler<P, R>,
        override val subject: Subject
    ) : Registration<RequestActionHandler<P, R>> {
        val metrics: Metrics = Metrics()
    }

    val requestActionHandlers =
        MapRegistrations<String, RequestActionHandler<Nothing, Nothing>, PacketActionHandlerRegistration<Nothing, Nothing>>()

    override suspend fun handle(context: PacketContext) {
        val packet: RequestPacket = context.packet as RequestPacket

        val requestModeHandlerRegistration = requestModeHandlers[packet.mode]
        if (requestModeHandlerRegistration == null) {
            val arguments = buildUnsupportedRequestModeArguments(packet.mode, requestModeHandlers.keys)
            val message = context.configuration.message.unsupportedRequestMode.format(arguments)

            context.api.send(
                ReceiptPacket(
                    id = randomPacketId(),
                    state = RECEIPT_STATE_FAILED,
                    request = packet.id,
                    cause = ErrorMessageCause(
                        error = UNSUPPORTED_REQUEST_MODE,
                        message = message,
                        context = arguments
                    )
                )
            )
            context.logger.warn { message }
            return
        }

        // Set cause.


        requestModeHandlerRegistration.value.handle(context)
    }

    private suspend fun doAction(context: PacketContext): ReceiptPacket? {
        val packet = context.packet as RequestPacket

        @Suppress("UNCHECKED_CAST")
        val requestActionHandlerRegistration =
            requestActionHandlers[packet.action] as PacketActionHandlerRegistration<Any?, Any?>?
        if (requestActionHandlerRegistration == null) {
            val arguments = buildUnsupportedRequestActionArguments(packet.action)
            val message = context.api.configuration.message.unsupportedRequestAction.format(arguments)

            context.api.send(
                ReceiptPacket(
                    id = randomPacketId(),
                    state = RECEIPT_STATE_FAILED,
                    request = packet.id,
                    cause = ErrorMessageCause(
                        error = UNSUPPORTED_REQUEST_MODE,
                        message = message,
                        context = arguments
                    )
                )
            )
            context.logger.warn { message }
            return null
        }

        // Create receipt packet for action handler to extend fields.
        val receipt = ReceiptPacket(
            id = randomPacketId(),
            state = RECEIPT_STATE_UNDEFINED,
            request = packet.id,
        )
        val requestActionContext = RequestActionContext(context.api, context.packet, receipt)

        // Deserialize argument.
        val argument = packet.raw.get(
            name = REQUEST_PACKET_FIELD_ARGUMENT,
            type = requestActionHandlerRegistration.action.requestArgument.type,
            optional = requestActionHandlerRegistration.action.requestArgument.optional,
            nullable = requestActionHandlerRegistration.action.requestArgument.nullable
        )

        // Handle the request action.
        val timeout = Duration.ofSeconds(packet.timeout)
        var durationSeconds = currentTimeSeconds
        var durationMillis = currentTimeMillis

        return try {
            // Handle packet.
            val result = withTimeout(timeout) {
                requestActionHandlerRegistration.value.handle(requestActionContext, argument)
            }

            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis
            context.logger.info {
                "Handle request action ${packet.type} succeed, " +
                        "cost ${durationSeconds}s (${durationMillis}ms)."
            }

            // Update receipt packet fields.
            when (result) {
                is SucceedRequestActionReceipt<*> -> receipt.apply {
                    raw[RECEIPT_PACKET_FIELD_STATE] = RECEIPT_STATE_SUCCEED
                    raw[RECEIPT_PACKET_FIELD_DATA] = result.data
                }

                is FailedRequestActionReceipt<*> -> receipt.apply {
                    raw[RECEIPT_PACKET_FIELD_STATE] = RECEIPT_STATE_FAILED
                    raw[PACKET_FIELD_CAUSE] = result.cause
                }
            }
        } catch (exception: TimeoutCancellationException) {
            val arguments = buildActionHandlerTimeoutArguments(packet.timeout)
            val message = context.configuration.message.actionHandlerTimeout.format(arguments)

            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis

            context.logger.warn {
                "Timeout occurred while handling request action: $packet " +
                        "after ${durationSeconds}s (${durationMillis}ms), expected: ${packet.timeout}s. " +
                        "The handler is registered by ${requestActionHandlerRegistration.subject}."
            }
            receipt.apply {
                raw[RECEIPT_PACKET_FIELD_STATE] = RECEIPT_STATE_INTERRUPTED
                raw[RECEIPT_PACKET_FIELD_DATA] = ErrorMessageCause(
                    error = ACTION_HANDLER_TIMEOUT,
                    message = message,
                    context = arguments
                )
            }
        } catch (exception: Exception) {
            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis

            // Log error.
            val message = context.api.configuration.message.internalActionHandlerError.toString()
            context.logger.error(exception) {
                "Exception occurred while handling request action:$packet " +
                        "after ${durationSeconds}s (${durationMillis}ms). " +
                        "The handler is registered by ${requestActionHandlerRegistration.subject}."
            }

            // Update receipt packet fields.
            receipt.apply {
                raw[RECEIPT_PACKET_FIELD_STATE] = RECEIPT_STATE_FAILED
                raw[RECEIPT_PACKET_FIELD_DATA] = ErrorMessageCause(
                    error = INTERNAL_ACTION_HANDLER_ERROR,
                    message = message,
                    context = emptyMap()
                )
            }
        }
    }
}
