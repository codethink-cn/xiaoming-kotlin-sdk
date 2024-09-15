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

@file:JvmName("Subjects")

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set

/**
 * Represent a subject can register listeners, send packets, and so on.
 *
 * @author Chuanwise
 */
abstract class Subject(
    raw: Raw
) : AbstractData(raw) {
    val type: String by raw

    @JvmOverloads
    constructor(
        type: String,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[SUBJECT_FIELD_TYPE] = type
    }
}

/**
 * Represent a subject that is a xiaoming SDK.
 *
 * @author Chuanwise
 */
class SdkSubject(
    raw: Raw
) : Subject(raw) {
    val group: String by raw
    val name: String by raw
    val version: Version by raw
    val protocol: Version by raw

    @JvmOverloads
    constructor(
        group: String,
        name: String,
        version: Version,
        protocol: Version,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[SUBJECT_FIELD_TYPE] = SUBJECT_TYPE_SDK
        raw[SDK_SUBJECT_FIELD_GROUP] = group
        raw[SDK_SUBJECT_FIELD_NAME] = name
        raw[SDK_SUBJECT_FIELD_VERSION] = version
        raw[SDK_SUBJECT_FIELD_PROTOCOL] = protocol
    }
}

/**
 * The current SDK subject.
 */
val CurrentSdkSubject: SdkSubject = SdkSubject(MapRaw().apply {
    this[SUBJECT_FIELD_TYPE] = SUBJECT_TYPE_SDK
    this[SDK_SUBJECT_FIELD_GROUP] = SdkGroup
    this[SDK_SUBJECT_FIELD_NAME] = SdkName
    this[SDK_SUBJECT_FIELD_VERSION] = SdkVersionString.toVersion()
    this[SDK_SUBJECT_FIELD_PROTOCOL] = SdkProtocolString.toVersion()
})

class ProtocolSubject(
    raw: Raw
) : Subject(raw) {
    val version: Version by raw

    @JvmOverloads
    constructor(
        version: Version,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[SUBJECT_FIELD_TYPE] = SUBJECT_TYPE_PROTOCOL
        raw[PROTOCOL_SUBJECT_FIELD_VERSION] = version
    }
}

/**
 * Represent a subject that is a module.
 *
 * @author Chuanwise
 */
class ModuleSubject(
    raw: Raw
) : Subject(raw) {
    val group: String by raw
    val name: String by raw
    val version: Version by raw

    @JvmOverloads
    constructor(
        group: String,
        name: String,
        version: Version,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[SUBJECT_FIELD_TYPE] = SUBJECT_TYPE_MODULE
        raw[MODULE_SUBJECT_FIELD_GROUP] = group
        raw[MODULE_SUBJECT_FIELD_NAME] = name
        raw[MODULE_SUBJECT_FIELD_VERSION] = version
    }
}

/**
 * The current protocol subject.
 */
val CurrentProtocolSubject: ProtocolSubject = ProtocolSubject(CurrentSdkSubject.protocol)

/**
 * Represent a subject that is a plugin.
 *
 * @author Chuanwise
 */
class PluginSubject(
    raw: Raw
) : Subject(raw) {
    val id: String by raw

    @JvmOverloads
    constructor(
        id: String,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[TYPE_FIELD_NAME] = SUBJECT_TYPE_PLUGIN
        raw[PACKET_FIELD_ID] = id
    }
}