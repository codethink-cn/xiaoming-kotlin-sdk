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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.Id
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.SubjectDescriptor
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.action.ConnectRequestPara
import cn.codethink.xiaoming.io.action.RequestHandler
import cn.codethink.xiaoming.io.connection.AuthorizationService
import cn.codethink.xiaoming.io.connection.EmptyAuthorizationService
import cn.codethink.xiaoming.io.connection.ServerApi

class ConnectionManagerApi(
    private val internalApi: LocalPlatformInternalApi
) {
    var authorizationService: AuthorizationService = EmptyAuthorizationService

    data class ServerRegistration(
        val keepOnConfigurationReload: Boolean,
        val keepOnNoAdapter: Boolean,
        override val subject: SubjectDescriptor,
        override val value: ServerApi
    ) : Registration<ServerApi>

    private val servers = MapRegistrations<Id, ServerApi, ServerRegistration>()

    // Associated by subject type.
    private val connectRequestHandlers = DefaultStringMapRegistrations<RequestHandler<ConnectRequestPara, Any?>>()

    fun getConnectRequestHandler(type: String): RequestHandler<ConnectRequestPara, Any?>? {
        return connectRequestHandlers[type]?.value
    }

    fun registerConnectRequestHandler(
        type: String,
        handler: RequestHandler<ConnectRequestPara, Any?>,
        subject: SubjectDescriptor
    ) {
        connectRequestHandlers.register(type, DefaultRegistration(handler, subject))
    }

    fun unregisterConnectRequestHandlerByType(type: String) {
        val effected = connectRequestHandlers.unregisterByKey(type) != null
        if (effected) {
            // Find if there are some server need to be stopped.
            ArrayList(servers.toMap().entries).forEach {
                if (it.value.subject.type == type && !it.value.keepOnNoAdapter) {
                    val registration = servers.unregisterByKey(it.key)
                    if (registration != null) {
                        registration.value.close(TextCause("Adapter unregistered.", it.value.subject))
                        internalApi.logger.info {
                            "Stopped server ${it.key} with subject ${it.value.subject} and type ${it.value.subject.type} due to adapter unregistered."
                        }
                    }
                }
            }
        }
    }
}