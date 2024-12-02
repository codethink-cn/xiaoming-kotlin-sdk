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

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.connection.SessionLanguageConfiguration
import cn.codethink.xiaoming.packet.SessionDescriptor
import com.fasterxml.jackson.annotation.JsonTypeName

const val CAUSE_TYPE_SESSION_INVALID = "session.invalid"
private val CAUSE_TYPE_SESSION_INVALID_ID = CAUSE_TYPE_SESSION_INVALID.toStringId()

@JsonTypeName(CAUSE_TYPE_UNSUPPORTED_SESSION)
class SessionInvalidCauseImpl : AbstractStringIdStandardCause, SessionInvalidCause {
    override var session: SessionDescriptor by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        session: SessionDescriptor,
        message: String,
        subject: SubjectDescriptor,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : super(
        CAUSE_TYPE_SESSION_INVALID_ID,
        message, subject, cause, raw
    ) {
        this.session = session
    }
}

fun SessionLanguageConfiguration.createSessionInvalidCause(
    session: SessionDescriptor, subject: SubjectDescriptor, cause: Cause? = null
): SessionInvalidCause = SessionInvalidCauseImpl(
    session, CAUSE_TYPE_SESSION_INVALID, subject, cause
).apply {
    message = invalidSession.format(raw)
}