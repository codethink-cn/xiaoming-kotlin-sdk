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

@file:JvmName("Errors")

package cn.codethink.xiaoming.io

import cn.codethink.xiaoming.common.toId

val ERROR_ADAPTER_NOT_FOUND = "adapter_not_found".toId()

val ERROR_UNSUPPORTED_PACKET_TYPE = "unsupported_packet_type".toId()

val ERROR_UNSUPPORTED_REQUEST_MODE = "unsupported_request_mode".toId()
val ERROR_UNSUPPORTED_REQUEST_ACTION = "unsupported_request_action".toId()

val ERROR_INTERNAL_ACTION_HANDLER_ERROR = "internal_action_handler_error".toId()
val ERROR_ACTION_HANDLER_TIMEOUT = "action_handler_timeout".toId()