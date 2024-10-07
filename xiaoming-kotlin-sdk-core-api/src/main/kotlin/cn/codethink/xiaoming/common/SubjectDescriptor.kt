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

@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.common

import cn.codethink.xiaoming.io.action.StandardAction
import cn.codethink.xiaoming.io.data.MapRaw
import cn.codethink.xiaoming.io.data.Raw
import cn.codethink.xiaoming.io.data.set
import com.fasterxml.jackson.annotation.JsonTypeName

/**
 * Represent a subject can register listeners, send packets, and so on.
 *
 * @author Chuanwise
 */
abstract class SubjectDescriptor : AbstractData {
    val type: String by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        type: String,
        raw: Raw = MapRaw()
    ) : super(raw) {
        raw[FIELD_TYPE] = type
    }
}

const val ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID = "id"

abstract class IdSubjectDescriptor : SubjectDescriptor {
    open val id: Id by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        type: String,
        id: Id,
        raw: Raw = MapRaw()
    ) : super(
        type = type,
        raw = raw
    ) {
        raw[ID_SUBJECT_DESCRIPTOR_DESCRIPTOR_FIELD_ID] = id
    }
}


const val SUBJECT_DESCRIPTOR_TYPE_PROTOCOL = "protocol"
const val PROTOCOL_SUBJECT_DESCRIPTOR_FIELD_VERSION = "version"

/**
 * Represent a subject that is a protocol. Type is [SUBJECT_DESCRIPTOR_TYPE_PROTOCOL].
 *
 * @author Chuanwise
 * @see StandardAction
 */
class ProtocolSubjectDescriptor : SubjectDescriptor {
    val version: Version by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        version: Version,
        raw: Raw = MapRaw()
    ) : super(
        type = SUBJECT_DESCRIPTOR_TYPE_PROTOCOL,
        raw = raw
    ) {
        raw[PROTOCOL_SUBJECT_DESCRIPTOR_FIELD_VERSION] = version
    }
}

/**
 * Represent a subject that is a module. Type is [SUBJECT_DESCRIPTOR_TYPE_MODULE].
 *
 * @author Chuanwise
 */
class ModuleSubjectDescriptor : SubjectDescriptor {
    val group: String by raw
    val name: String by raw
    val version: Version by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        group: String,
        name: String,
        version: Version,
        raw: Raw = MapRaw()
    ) : super(
        type = FIELD_TYPE,
        raw = raw
    ) {
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_GROUP] = group
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_NAME] = name
        raw[MODULE_SUBJECT_DESCRIPTOR_FIELD_VERSION] = version
    }
}

/**
 * The current protocol subject.
 */
val XiaomingProtocolSubject: ProtocolSubjectDescriptor = ProtocolSubjectDescriptor(SdkProtocol)

const val SUBJECT_DESCRIPTOR_TYPE_PLUGIN = "plugin"

/**
 * Represent a subject that is a plugin. Type is [SUBJECT_DESCRIPTOR_TYPE_PLUGIN].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_PLUGIN)
class PluginSubjectDescriptor : IdSubjectDescriptor {
    override val id: NamespaceId by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: NamespaceId,
        raw: Raw = MapRaw()
    ) : super(
        type = SUBJECT_DESCRIPTOR_TYPE_PLUGIN,
        id = id,
        raw = raw
    )
}

const val SUBJECT_DESCRIPTOR_TYPE_PLATFORM = "platform"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_DESCRIPTOR_TYPE_PLATFORM].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_PLATFORM)
class PlatformSubjectDescriptor : IdSubjectDescriptor {
    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : super(
        type = SUBJECT_DESCRIPTOR_TYPE_PLATFORM,
        id = id,
        raw = raw
    )
}

const val SUBJECT_DESCRIPTOR_TYPE_CONNECTION = "connection"

/**
 * Represent a subject that is a platform. Type is [SUBJECT_DESCRIPTOR_TYPE_CONNECTION].
 *
 * @author Chuanwise
 */
@JsonTypeName(SUBJECT_DESCRIPTOR_TYPE_CONNECTION)
class ConnectionSubjectDescriptor : IdSubjectDescriptor {
    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Id,
        raw: Raw = MapRaw()
    ) : super(
        type = SUBJECT_DESCRIPTOR_TYPE_CONNECTION,
        id = id,
        raw = raw
    )
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
class DefaultPluginSubjectMatcher : AbstractData, Matcher<SubjectDescriptor> {
    override val type: String by raw

    val id: Matcher<NamespaceId> by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        id: Matcher<NamespaceId>,
        raw: Raw = MapRaw()
    ) : super(raw) {
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
