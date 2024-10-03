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

import cn.codethink.xiaoming.io.data.Field
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.io.data.setValue
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeName

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
@JsonTypeName(SUBJECT_TYPE_PROTOCOL)
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
val XiaomingSdkSubject: SdkSubject = SdkSubject(MapRaw().apply {
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
    val matcher = ProtocolSubjectMatcher()

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
 * Represent a subject that is a module. Type is [SUBJECT_TYPE_MODULE].
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
val XiaomingProtocolSubject: ProtocolSubject = ProtocolSubject(XiaomingSdkSubject.protocol)

const val SUBJECT_TYPE_PLUGIN = "plugin"
const val PLUGIN_SUBJECT_FIELD_ID = "id"

/**
 * Represent a subject that is a plugin. Type is [SUBJECT_TYPE_PLUGIN].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_TYPE_PLUGIN)
class PluginSubject(
    raw: Raw
) : Subject(raw) {
    val id: SegmentId by raw

    @JvmOverloads
    constructor(
        id: SegmentId,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_TYPE_PLUGIN
        raw[PLUGIN_SUBJECT_FIELD_ID] = id
    }
}

const val SUBJECT_TYPE_PLATFORM = "platform"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_TYPE_PLATFORM].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_TYPE_PLATFORM)
class PlatformSubject(
    raw: Raw
) : Subject(raw) {
    var id: Id by raw

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_TYPE_PLATFORM
        this.id = id
    }
}

const val SUBJECT_TYPE_CONNECTION = "connection"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_TYPE_CONNECTION].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_TYPE_CONNECTION)
class ConnectionSubject(
    raw: Raw
) : Subject(raw) {
    var id: Id by raw

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_TYPE_CONNECTION
        this.id = id
    }
}

/**
 * Used to match plugin subject from [Subject]. Type is
 * [DEFAULT_PLUGIN_SUBJECT_MATCHER_FIELD_ID_MATCHER].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_MATCHER_TYPE_DEFAULT_PLUGIN)
class DefaultPluginSubjectMatcher(
    raw: Raw
) : AbstractData(raw), Matcher<Subject> {
    override val type: String by raw

    @JsonIgnore
    override val targetType: Class<Subject> = Subject::class.java

    @JsonIgnore
    override val targetNullable: Boolean = false

    @Field(DEFAULT_PLUGIN_SUBJECT_MATCHER_FIELD_ID_MATCHER)
    val idMatcher: Matcher<SegmentId> by raw

    @JvmOverloads
    constructor(
        idMatcher: Matcher<SegmentId>,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = SUBJECT_MATCHER_TYPE_DEFAULT_PLUGIN
        raw[DEFAULT_PLUGIN_SUBJECT_MATCHER_FIELD_ID_MATCHER] = idMatcher
    }

    override fun isMatched(target: Subject): Boolean {
        if (target.type != SUBJECT_TYPE_PLUGIN) {
            return false
        }
        val pluginSubject = target as PluginSubject

        return idMatcher.isMatched(pluginSubject.id)
    }
}

fun PluginSubject.toLiteralMatcher() = DefaultPluginSubjectMatcher(id.toLiteralMatcher())


class ProtocolSubjectMatcher @JvmOverloads constructor(
    raw: Raw = MapRaw()
) : AbstractData(raw), Matcher<ProtocolSubject> {
    override val type: String by raw

    @JsonIgnore
    override val targetType: Class<ProtocolSubject> = ProtocolSubject::class.java

    @JsonIgnore
    override val targetNullable: Boolean = false

    init {
        raw[MATCHER_FIELD_TYPE] = SUBJECT_MATCHER_TYPE_DEFAULT_PROTOCOL
    }

    override fun isMatched(target: ProtocolSubject): Boolean {
        return true
    }
}