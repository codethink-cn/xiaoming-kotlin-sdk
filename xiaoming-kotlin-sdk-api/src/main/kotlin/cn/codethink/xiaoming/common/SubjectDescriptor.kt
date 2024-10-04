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

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.getValue
import cn.codethink.xiaoming.io.data.set
import cn.codethink.xiaoming.io.data.setValue
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Represent a subject can register listeners, send packets, and so on.
 *
 * @author Chuanwise
 */
abstract class SubjectDescriptor(
    raw: Raw
) : AbstractData(raw) {
    val type: String by raw

    @JvmOverloads
    constructor(
        type: String,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = type
    }
}

const val ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID = "id"

abstract class IdSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
    open val id: Id by raw

    @JvmOverloads
    constructor(
        type: String,
        id: Id,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = type
        raw[ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID] = id
    }
}


const val SUBJECT_DESCRIPTOR_TYPE_PROTOCOL = "protocol"
const val PROTOCOL_SUBJECT_DESCRIPTOR_FIELD_VERSION = "version"

/**
 * Represent a subject that is a xiaoming SDK.
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_PROTOCOL)
class SdkSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
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
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_SDK
        raw[SDK_SUBJECT_DESCRIPTOR_FIELD_GROUP] = group
        raw[SDK_SUBJECT_DESCRIPTOR_FIELD_NAME] = name
        raw[SDK_SUBJECT_DESCRIPTOR_FIELD_VERSION] = version
        raw[SDK_SUBJECT_DESCRIPTOR_FIELD_PROTOCOL] = protocol
    }
}

/**
 * The current SDK subject.
 */
val XiaomingSdkSubject: SdkSubjectDescriptor = SdkSubjectDescriptor(MapRaw().apply {
    this[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_SDK
    this[SDK_SUBJECT_DESCRIPTOR_FIELD_GROUP] = SdkGroup
    this[SDK_SUBJECT_DESCRIPTOR_FIELD_NAME] = SdkName
    this[SDK_SUBJECT_DESCRIPTOR_FIELD_VERSION] = SdkVersionString.toVersion()
    this[SDK_SUBJECT_DESCRIPTOR_FIELD_PROTOCOL] = SdkProtocolString.toVersion()
})

class ProtocolSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
    val version: Version by raw
    val matcher = ProtocolSubjectMatcher()

    @JvmOverloads
    constructor(
        version: Version,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_PROTOCOL
        raw[PROTOCOL_SUBJECT_DESCRIPTOR_FIELD_VERSION] = version
    }
}

/**
 * Represent a subject that is a module. Type is [SUBJECT_DESCRIPTOR_TYPE_MODULE].
 *
 * @author Chuanwise
 */
class ModuleSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
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
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_MODULE
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_GROUP] = group
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_NAME] = name
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_VERSION] = version
    }
}

/**
 * The current protocol subject.
 */
val XiaomingProtocolSubject: ProtocolSubjectDescriptor = ProtocolSubjectDescriptor(XiaomingSdkSubject.protocol)

const val SUBJECT_DESCRIPTOR_TYPE_PLUGIN = "plugin"

/**
 * Represent a subject that is a plugin. Type is [SUBJECT_DESCRIPTOR_TYPE_PLUGIN].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_PLUGIN)
class PluginSubjectDescriptor(
    raw: Raw
) : IdSubjectDescriptor(raw) {
    override val id: SegmentId by raw

    @JvmOverloads
    constructor(
        id: SegmentId,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_PLUGIN
        raw[ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID] = id
    }
}

const val SUBJECT_DESCRIPTOR_TYPE_PLATFORM = "platform"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_DESCRIPTOR_TYPE_PLATFORM].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_PLATFORM)
class PlatformSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
    var id: Id by raw

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_PLATFORM
        this.id = id
    }
}

const val SUBJECT_DESCRIPTOR_TYPE_CONNECTION = "connection"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_DESCRIPTOR_TYPE_CONNECTION].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_CONNECTION)
class ConnectionSubjectDescriptor(
    raw: Raw
) : SubjectDescriptor(raw) {
    var id: Id by raw

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[FIELD_TYPE] = SUBJECT_DESCRIPTOR_TYPE_CONNECTION
        this.id = id
    }
}

const val SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN = "subject.plugin.default"
const val DEFAULT_PLUGIN_SUBJECT_DESCRIPTOR_MATCHER_FIELD_ID = "id"

/**
 * Used to match plugin subject from [SubjectDescriptor]. Type is
 * [DEFAULT_PLUGIN_SUBJECT_DESCRIPTOR_MATCHER_FIELD_ID].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN)
class DefaultPluginSubjectMatcher(
    raw: Raw
) : AbstractData(raw), Matcher<SubjectDescriptor> {
    override val type: String by raw

    override val targetType: Class<SubjectDescriptor> = SubjectDescriptor::class.java

    override val targetNullable: Boolean = false

    //    @Field(DEFAULT_PLUGIN_SUBJECT_DESCRIPTOR_MATCHER_FIELD_ID_MATCHER)
    val id: Matcher<SegmentId> by raw

    @JvmOverloads
    constructor(
        id: Matcher<SegmentId>,
        raw: Raw = MapRaw()
    ) : this(raw) {
        raw[MATCHER_FIELD_TYPE] = SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PLUGIN
        raw[DEFAULT_PLUGIN_SUBJECT_DESCRIPTOR_MATCHER_FIELD_ID] = id
    }

    override fun isMatched(target: SubjectDescriptor): Boolean {
        if (target.type != SUBJECT_DESCRIPTOR_TYPE_PLUGIN) {
            return false
        }
        val pluginSubject = target as PluginSubjectDescriptor

        return id.isMatched(pluginSubject.id)
    }
}

fun PluginSubjectDescriptor.toLiteralMatcher() = DefaultPluginSubjectMatcher(id.toLiteralMatcher())


class ProtocolSubjectMatcher @JvmOverloads constructor(
    raw: Raw = MapRaw()
) : AbstractData(raw), Matcher<ProtocolSubjectDescriptor> {
    override val type: String by raw

    override val targetType: Class<ProtocolSubjectDescriptor> = ProtocolSubjectDescriptor::class.java

    override val targetNullable: Boolean = false

    init {
        raw[MATCHER_FIELD_TYPE] = SUBJECT_DESCRIPTOR_MATCHER_TYPE_DEFAULT_PROTOCOL
    }

    override fun isMatched(target: ProtocolSubjectDescriptor): Boolean {
        return true
    }
}