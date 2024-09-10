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
import java.util.Properties

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
 * Represent a subject that is a xiaoming protocol.
 *
 * @author Chuanwise
 */
class ProtocolSubject(
    raw: Raw
) : Subject(SUBJECT_TYPE_PROTOCOL) {
    val group: String by raw
    val name: String by raw
    val version: Version by raw
}

/**
 * The current protocol subject.
 */
val CurrentProtocolSubject: ProtocolSubject = ProtocolSubject(MapRaw().apply {
    val properties = Properties().apply {
        load(ProtocolSubject::class.java.classLoader.getResourceAsStream("xiaoming.properties"))
    }

    this[SUBJECT_FIELD_TYPE] = SUBJECT_TYPE_PROTOCOL

    val group: String by properties
    val name: String by properties
    val version: String by properties

    this[PROTOCOL_SUBJECT_GROUP] = group
    this[PROTOCOL_SUBJECT_NAME] = name
    this[PROTOCOL_SUBJECT_FIELD_VERSION] = versionOf(version)
})

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