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

@file:JvmName("Constants")

package cn.codethink.xiaoming.common

const val TYPE_FIELD_NAME = "type"

const val PACKET_FIELD_ID = "id"
const val PACKET_FIELD_TYPE = "type"
const val PACKET_FIELD_TIME = "time"
const val PACKET_FIELD_CAUSE = "cause"

const val PACKET_TYPE_REQUEST = "request"
const val REQUEST_PACKET_FIELD_ACTION = "action"
const val REQUEST_PACKET_FIELD_SUBJECT = "subject"
const val REQUEST_PACKET_FIELD_ARGUMENT = "argument"
const val REQUEST_PACKET_FIELD_MODE = "mode"
const val REQUEST_PACKET_FIELD_TIMEOUT = "timeout"
const val RECEIPT_PACKET_FIELD_STATE = "state"
const val RECEIPT_PACKET_FIELD_REQUEST = "request"
const val RECEIPT_PACKET_FIELD_DATA = "data"
const val REQUEST_MODE_SYNC = "sync"
const val REQUEST_MODE_ASYNC = "async"
const val REQUEST_MODE_ASYNC_RESULT = "async_result"

const val PACKET_TYPE_RECEIPT = "receipt"
const val RECEIPT_STATE_UNDEFINED = "undefined"
const val RECEIPT_STATE_SUCCEED = "succeed"
const val RECEIPT_STATE_FAILED = "failed"
const val RECEIPT_STATE_RECEIVED = "received"
const val RECEIPT_STATE_CANCELLED = "cancelled"
const val RECEIPT_STATE_INTERRUPTED = "interrupted"

const val SUBJECT_FIELD_TYPE = TYPE_FIELD_NAME
const val SUBJECT_TYPE_PLUGIN = "plugin"

const val SUBJECT_TYPE_PROTOCOL = "protocol"
const val PROTOCOL_SUBJECT_FIELD_VERSION = "version"
const val PROTOCOL_SUBJECT_GROUP = "group"
const val PROTOCOL_SUBJECT_NAME = "name"

const val CAUSE_FIELD_TYPE = TYPE_FIELD_NAME
const val CAUSE_FIELD_CAUSE = "cause"
const val CAUSE_TYPE_TEXT = "text"
const val TEXT_CAUSE_FIELD_TEXT = "text"

const val CAUSE_TYPE_ERROR_TEXT = "error_message"
const val ERROR_TEXT_CAUSE_FIELD_ERROR = "error"
const val ERROR_TEXT_CAUSE_FIELD_MESSAGE = "message"
const val ERROR_TEXT_CAUSE_FIELD_CONTEXT = "context"

const val CAUSE_TYPE_PACKET_DATA = "packet_data"
const val CAUSE_TYPE_PACKET_ID = "packet_id"
const val PACKET_DATA_CAUSE_FIELD_PACKET = "packet"
const val PACKET_CAUSE_FIELD_ID = "id"

const val PUBLISH_EVENT_REQUEST_ACTION = "publish-event"
const val PUBLISH_EVENT_REQUEST_PARA_TYPE = "type"
const val PUBLISH_EVENT_REQUEST_PARA_EVENT = "event"
const val PUBLISH_EVENT_REQUEST_PARA_MUTABLE = "mutable"

const val EVENT_SNAPSHOT_FIELD_EVENT = "event"
const val EVENT_SNAPSHOT_FIELD_LISTENER = "listener"
const val EVENT_SNAPSHOT_FIELD_CAUSE = "cause"

const val PUBLISH_EVENT_RECEIPT_DATA_SNAPSHOTS = "snapshots"

const val LISTENER_DESCRIPTOR_FIELD_ID = "id"
const val LISTENER_DESCRIPTOR_FIELD_SUBJECT = "subject"