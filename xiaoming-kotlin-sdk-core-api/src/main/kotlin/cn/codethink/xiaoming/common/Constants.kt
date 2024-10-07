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

const val FIELD_TYPE = "type"
const val FIELD_VERSION = "version"
const val FIELD_CAUSE = "cause"
const val FIELD_SUBJECT = "subject"

const val REQUEST_PACKET_FIELD_ARGUMENT = "argument"

const val REQUEST_MODE_SYNC = "sync"
const val REQUEST_MODE_ASYNC = "async"

const val PACKET_TYPE_RECEIPT = "receipt"
const val RECEIPT_PACKET_FIELD_DATA = "data"

const val RECEIPT_STATE_UNDEFINED = "undefined"
const val RECEIPT_STATE_SUCCEED = "succeed"
const val RECEIPT_STATE_FAILED = "failed"
const val RECEIPT_STATE_RECEIVED = "received"
const val RECEIPT_STATE_INTERRUPTED = "interrupted"

const val SUBJECT_DESCRIPTOR_TYPE_SDK = "sdk"
const val SDK_SUBJECT_DESCRIPTOR_FIELD_VERSION = "version"
const val SDK_SUBJECT_DESCRIPTOR_FIELD_GROUP = "group"
const val SDK_SUBJECT_DESCRIPTOR_FIELD_NAME = "name"
const val SDK_SUBJECT_DESCRIPTOR_FIELD_PROTOCOL = "protocol"

const val SUBJECT_DESCRIPTOR_TYPE_MODULE = "module"
const val MODULE_SUBJECT_DESCRIPTOR_FIELD_VERSION = "version"
const val MODULE_SUBJECT_DESCRIPTOR_FIELD_GROUP = "group"
const val MODULE_SUBJECT_DESCRIPTOR_FIELD_NAME = "name"

const val CAUSE_TYPE_STANDARD_TEXT = "standard_text"


const val PUBLISH_EVENT_REQUEST_PARA_TYPE = "type"
const val PUBLISH_EVENT_REQUEST_PARA_EVENT = "event"
const val PUBLISH_EVENT_REQUEST_PARA_MUTABLE = "mutable"

const val EVENT_SNAPSHOT_FIELD_EVENT = "event"
const val EVENT_SNAPSHOT_FIELD_LISTENER = "listener"
const val EVENT_SNAPSHOT_FIELD_CAUSE = "cause"

const val PUBLISH_EVENT_RECEIPT_DATA_SNAPSHOTS = "snapshots"

const val PERMISSION_SUBJECT_DESCRIPTOR_FIELD_NODE = "node"
const val PERMISSION_SUBJECT_DESCRIPTOR_FIELD_SUBJECT = "subject"

const val PERMISSION_VARIABLE_META_FIELD_OPTIONAL = "optional"
const val PERMISSION_VARIABLE_META_FIELD_NULLABLE = "nullable"
const val PERMISSION_VARIABLE_META_FIELD_DEFAULT_MATCHER_OR_VALUE = "default_matcher_or_value"
const val PERMISSION_VARIABLE_META_FIELD_DESCRIPTION = "description"

const val PERMISSION_META_FIELD_NODE = "node"
const val PERMISSION_META_FIELD_SUBJECT = "subject"
const val PERMISSION_META_FIELD_PARAMETERS = "parameters"
const val PERMISSION_META_FIELD_DESCRIPTION = "description"
const val PERMISSION_META_FIELD_DESCRIPTOR = "descriptor"

const val PERMISSION_FIELD_DESCRIPTOR = "descriptor"
const val PERMISSION_FIELD_ARGUMENTS = "arguments"

const val MATCHER_FIELD_TYPE = FIELD_TYPE

const val SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PROTOCOL = "subject.protocol"

const val PERMISSION_MATCHER_TYPE_DEFAULT = "permission.default"
const val DEFAULT_PERMISSION_MATCHER_FIELD_NODE_MATCHER = "node_matcher"
const val DEFAULT_PERMISSION_MATCHER_FIELD_ARGUMENT_MATCHERS = "arguments"

const val PERMISSION_MATCHER_TYPE_LITERAL = "permission.literal"
const val LITERAL_PERMISSION_MATCHER_FIELD_VALUE = "value"

const val STRING_MATCHER_TYPE_WILDCARD = "string.wildcard"
const val STRING_MATCHER_TYPE_REGEX = "string.regex"
const val STRING_MATCHER_TYPE_LITERAL = "string.literal"

const val SEGMENT_ID_MATCHER_TYPE_DEFAULT = "segment_id.default"
const val SEGMENT_ID_MATCHER_TYPE_LITERAL = "segment_id.literal"

const val HEADER_KEY_AUTHORIZATION = "Authorization"
const val HEADER_VALUE_AUTHORIZATION_BEARER_WITH_SPACE = "Bearer "