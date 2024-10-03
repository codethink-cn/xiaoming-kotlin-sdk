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
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.RECEIPT_STATE_FAILED
import cn.codethink.xiaoming.common.RECEIPT_STATE_INTERRUPTED
import cn.codethink.xiaoming.common.RECEIPT_STATE_RECEIVED
import cn.codethink.xiaoming.common.RECEIPT_STATE_SUCCEED
import cn.codethink.xiaoming.common.RECEIPT_STATE_UNDEFINED
import cn.codethink.xiaoming.common.REQUEST_MODE_ASYNC
import cn.codethink.xiaoming.common.REQUEST_MODE_SYNC
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.StandardTextCause
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.currentTimeMillis
import cn.codethink.xiaoming.common.currentTimeSeconds
import cn.codethink.xiaoming.io.ERROR_ACTION_HANDLER_TIMEOUT
import cn.codethink.xiaoming.io.ERROR_INTERNAL_ACTION_HANDLER_ERROR
import cn.codethink.xiaoming.io.ERROR_UNSUPPORTED_REQUEST_MODE
import cn.codethink.xiaoming.io.ProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.action.Action
import cn.codethink.xiaoming.io.action.FailedRequestResult
import cn.codethink.xiaoming.io.action.InterruptedRequestResult
import cn.codethink.xiaoming.io.action.PacketRequestContext
import cn.codethink.xiaoming.io.action.RequestHandler
import cn.codethink.xiaoming.io.action.SucceedRequestResult
import cn.codethink.xiaoming.io.buildActionHandlerTimeoutArguments
import cn.codethink.xiaoming.io.buildUnsupportedRequestActionArguments
import cn.codethink.xiaoming.io.buildUnsupportedRequestModeArguments
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.time.withTimeout
import java.time.Duration

/**
 * Handle the [RequestPacket]s.
 *
 * @author Chuanwise
 */
class RequestPacketHandler(
    private val language: ProtocolLanguageConfiguration,
    subject: Subject
) : PacketHandler {
    inner class SyncRequestPacketHandler : PacketHandler {
        override suspend fun handle(context: PacketContext) {
            dispatchActionAndGetReceipt(context)?.let {
                context.send(it)
            }
        }
    }

    inner class AsyncRequestPacketHandler : PacketHandler {
        override suspend fun handle(context: PacketContext) {
            context.send(ReceiptPacket(
                id = randomUuidString(),
                target = context.received.packet.id,
                state = RECEIPT_STATE_RECEIVED,
                subject = context.connection.subject,
                session = context.connection.session,
            ))

            dispatchActionAndGetReceipt(context)
        }
    }

    private val modes = MapRegistrations<String, PacketHandler, DefaultRegistration<PacketHandler>>()

    inner class PacketActionHandlerRegistration<P, R>(
        val action: Action<P, R>,
        override val value: RequestHandler<P, R>,
        override val subject: Subject
    ) : Registration<RequestHandler<P, R>>

    private val actions = MapRegistrations<String, RequestHandler<Nothing, Nothing>, PacketActionHandlerRegistration<Nothing, Nothing>>()

    init {
        registerModeHandler(REQUEST_MODE_SYNC, SyncRequestPacketHandler(), subject)
        registerModeHandler(REQUEST_MODE_ASYNC, AsyncRequestPacketHandler(), subject)
    }

    override suspend fun handle(context: PacketContext) {
        val packet: RequestPacket = context.received.packet as RequestPacket

        val requestModeHandlerRegistration = modes[packet.mode]
        if (requestModeHandlerRegistration == null) {
            val arguments = buildUnsupportedRequestModeArguments(packet.mode, modes.toMap().keys)
            val message = language.unsupportedRequestMode.format(arguments)

            context.send(
                ReceiptPacket(
                    id = randomUuidString(),
                    target = packet.id,
                    state = RECEIPT_STATE_FAILED,
                    subject = context.connection.subject,
                    cause = StandardTextCause(
                        id = ERROR_UNSUPPORTED_REQUEST_MODE,
                        text = message,
                        arguments = arguments
                    ),
                    session = context.connection.session
                )
            )
            context.logger.warn { message }
            return
        }

        // Set cause.
        requestModeHandlerRegistration.value.handle(context)
    }

    private suspend fun dispatchActionAndGetReceipt(context: PacketContext): ReceiptPacket? {
        val request = context.received.packet as RequestPacket

        @Suppress("UNCHECKED_CAST")
        val registration = actions[request.action] as PacketActionHandlerRegistration<Any?, Any?>?
        if (registration == null) {
            val arguments = buildUnsupportedRequestActionArguments(request.action)
            val message = language.unsupportedRequestAction.format(arguments)

            context.send(
                ReceiptPacket(
                    id = randomUuidString(),
                    target = request.id,
                    state = RECEIPT_STATE_FAILED,
                    subject = context.connection.subject,
                    cause = StandardTextCause(
                        id = ERROR_UNSUPPORTED_REQUEST_MODE,
                        text = message,
                        arguments = arguments
                    ),
                    session = context.connection.session
                )
            )
            context.logger.warn { message }
            return null
        }

        // Create receipt packet for action handler to extend fields.
        val receipt = ReceiptPacket(
            id = randomUuidString(),
            target = request.id,
            state = RECEIPT_STATE_UNDEFINED,
            subject = context.connection.subject,
            session = context.connection.session
        )

        // Handle the request action.
        val timeout = Duration.ofSeconds(request.timeout)
        var durationSeconds = currentTimeSeconds
        var durationMillis = currentTimeMillis

        return try {
            // Handle packet.
            val result = withTimeout(timeout) {
                registration.value.handle(
                    PacketRequestContext(
                        action = registration.action,
                        request = request,
                        defaultSubject = context.connection.subject,
                        receipt = receipt,
                        connection = context.connection
                    )
                )
            }

            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis
            context.logger.debug {
                "Handle request action ${request.type} returns $result, cost ${durationSeconds}s (${durationMillis}ms)."
            }

            // Update receipt packet fields.
            when (result) {
                is SucceedRequestResult -> receipt.apply {
                    state = RECEIPT_STATE_SUCCEED
                    data = result.data
                }

                is FailedRequestResult -> receipt.apply {
                    state = RECEIPT_STATE_FAILED
                    cause = result.cause
                }

                is InterruptedRequestResult -> receipt.apply {
                    state = RECEIPT_STATE_INTERRUPTED
                    cause = result.cause
                }
            }
        } catch (exception: TimeoutCancellationException) {
            val arguments = buildActionHandlerTimeoutArguments(request.timeout)
            val message = language.actionHandlerTimeout.format(arguments)

            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis

            context.logger.warn {
                "Timeout occurred while handling request action: $request " +
                        "after ${durationSeconds}s (${durationMillis}ms), expected: ${request.timeout}s. " +
                        "The handler is registered by ${registration.subject}."
            }
            receipt.apply {
                state = RECEIPT_STATE_INTERRUPTED
                data = StandardTextCause(
                    id = ERROR_ACTION_HANDLER_TIMEOUT,
                    text = message,
                    arguments = arguments
                )
            }
        } catch (exception: Exception) {
            // Calculate duration and log it.
            durationSeconds = currentTimeSeconds - durationSeconds
            durationMillis = currentTimeSeconds - durationMillis

            // Log error.
            val message = language.internalActionHandlerError.toString()
            context.logger.error(exception) {
                "Exception occurred while handling request action:$request " +
                        "after ${durationSeconds}s (${durationMillis}ms). " +
                        "The handler is registered by ${registration.subject}."
            }

            // Update receipt packet fields.
            receipt.apply {
                state = RECEIPT_STATE_FAILED
                data = StandardTextCause(
                    id = ERROR_INTERNAL_ACTION_HANDLER_ERROR,
                    text = message,
                    arguments = emptyMap()
                )
            }
        }
    }

    fun registerModeHandler(mode: String, handler: PacketHandler, subject: Subject) {
        modes.register(mode, DefaultRegistration(handler, subject))
    }

    fun unregisterModeHandlerByMode(mode: String) {
        modes.unregisterByKey(mode)
    }

    fun unregisterModeHandlerBySubject(subject: Subject) {
        modes.unregisterBySubject(subject)
    }

    @Suppress("UNCHECKED_CAST")
    fun <P, R> registerActionHandler(action: Action<P, R>, handler: RequestHandler<P, R>, subject: Subject) {
        actions.register(action.name, PacketActionHandlerRegistration(action, handler, subject) as PacketActionHandlerRegistration<Nothing, Nothing>)
    }

    fun unregisterActionHandlerByAction(action: String) {
        actions.unregisterByKey(action)
    }

    fun unregisterActionHandlerBySubject(subject: Subject) {
        actions.unregisterBySubject(subject)
    }

    fun unregisterBySubject(subject: Subject) {
        unregisterModeHandlerBySubject(subject)
        unregisterActionHandlerBySubject(subject)
    }
}
