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

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.InternalApi
import cn.codethink.xiaoming.common.MapRegistrations
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.TextCause
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.LocalPlatformInternalState
import cn.codethink.xiaoming.internal.assertState
import cn.codethink.xiaoming.io.connection.Authorizer
import cn.codethink.xiaoming.io.connection.EmptyAuthorizer
import cn.codethink.xiaoming.io.connection.Server
import kotlin.concurrent.read

class ConnectionManagerApi(
    private val internalApi: LocalPlatformInternalApi
) {
    var authorizer: Authorizer = EmptyAuthorizer

    data class ServerRegistration(
        val keepOnConfigurationReload: Boolean,
        val keepOnNoAdapter: Boolean,
        override val subject: Subject,
        override val value: Server
    ) : Registration<Server>

    private val servers = MapRegistrations<String, Server, ServerRegistration>()

    // Associated by subject type.
    private val adapters = DefaultStringMapRegistrations<ConnectionAdapter>()

    @InternalApi
    fun onStart(cause: Cause, subject: Subject) = internalApi.lock.read {
        // Register plugin enable and disable event listener.
    }

    @InternalApi
    fun onConfigurationReload(cause: Cause, subject: Subject) = internalApi.lock.read {
        internalApi.assertState(LocalPlatformInternalState.STARTING)

        val connections = internalApi.platformConfiguration.connections
        if (!connections.keepConnections) {
            val keys = ArrayList(servers.toMap().keys)
            keys.forEach {
                val registration = servers.unregisterByKey(it)
                if (registration != null) {
                    registration.value.close(cause, subject)
                    internalApi.logger.warn { "Close server $it because of configuration reload and `keepConnections = false`." }
                }
            }
        }

        // Close servers that are not in the configuration.
        ArrayList(servers.toMap().entries).forEach {
            // Check if item removed.
            if (it.key in connections.servers) {
                // Check if item's subject changed.
                val configurationItem = connections.servers[it.key]!!
                if (it.value.value.subject != configurationItem.subject) {
                    val registration = servers.unregisterByKey(it.key)
                    if (registration != null) {
                        registration.value.close(cause, subject)
                        internalApi.logger.warn {
                            "Close server ${it.key} because of configuration reload and its subject is changed " +
                                    "from ${it.value.value.subject} to ${configurationItem.subject}."
                        }
                    }
                }
                if (!it.value.keepOnConfigurationReload && !configurationItem.enable) {
                    val registration = servers.unregisterByKey(it.key)
                    if (registration != null) {
                        registration.value.close(cause, subject)
                        internalApi.logger.warn {
                            "Close server ${it.key} because of configuration reload and this item is disabled."
                        }
                    }
                }
            } else {
                if (!it.value.keepOnConfigurationReload) {
                    val registration = servers.unregisterByKey(it.key)
                    if (registration != null) {
                        registration.value.close(cause, subject)
                        internalApi.logger.warn {
                            "Close server ${it.key} because of configuration reload and this item is removed."
                        }
                    }
                }
            }
        }

        // Check if server can be enabled.
        connections.servers.forEach {
            if (servers[it.key] == null && it.value.enable) {
                val registration = adapters[it.value.subject.type]
                if (registration == null) {
                    internalApi.logger.warn {
                        "Cannot enable server ${it.key} because there is no adapter for subject type ${it.value.subject.type}."
                    }
                } else {
                    val server = it.value.toServer(internalApi)
                    servers.register(
                        it.key, ServerRegistration(
                            keepOnConfigurationReload = false,
                            keepOnNoAdapter = false,
                            subject = it.value.subject,
                            value = server
                        )
                    )
                    internalApi.logger.info {
                        "Started server ${it.key} with subject ${it.value.subject} and type ${it.value.subject.type} due to configuration reloaded."
                    }
                }
            }
        }
    }

    fun getConnectionAdapter(type: String): ConnectionAdapter? = adapters[type]?.value

    fun registerConnectionAdapter(type: String, adapter: ConnectionAdapter, subject: Subject) {
        val effected = adapters[type] == null
        adapters.register(type, DefaultRegistration(adapter, subject))

        if (effected) {
            // Find if there are some server need to be started.
            val connections = internalApi.platformConfiguration.connections
            connections.servers.forEach {
                if (it.value.subject.type == type && it.value.enable && servers[it.key] == null) {
                    val server = it.value.toServer(internalApi)
                    servers.register(
                        it.key, ServerRegistration(
                            keepOnConfigurationReload = false,
                            keepOnNoAdapter = false,
                            subject = it.value.subject,
                            value = server
                        )
                    )
                    internalApi.logger.info {
                        "Started server ${it.key} with subject ${it.value.subject} and type ${it.value.subject.type} due to adapter registered."
                    }
                }
            }
        }
    }

    fun unregisterConnectionAdapterByType(type: String) {
        val effected = adapters.unregisterByKey(type) != null
        if (effected) {
            // Find if there are some server need to be stopped.
            ArrayList(servers.toMap().entries).forEach {
                if (it.value.subject.type == type && !it.value.keepOnNoAdapter) {
                    val registration = servers.unregisterByKey(it.key)
                    if (registration != null) {
                        registration.value.close(TextCause("Adapter unregistered."), it.value.subject)
                        internalApi.logger.info {
                            "Stopped server ${it.key} with subject ${it.value.subject} and type ${it.value.subject.type} due to adapter unregistered."
                        }
                    }
                }
            }
        }
    }
}