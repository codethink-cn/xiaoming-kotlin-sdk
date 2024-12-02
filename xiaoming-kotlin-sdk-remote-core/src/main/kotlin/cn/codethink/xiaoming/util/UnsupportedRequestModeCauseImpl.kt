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
import com.fasterxml.jackson.annotation.JsonTypeName

const val CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE = "request.mode.unsupported"
private val CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE_ID = CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE.toStringId()

@JsonTypeName(CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE)
class UnsupportedRequestModeCauseImpl : AbstractStringIdStandardCause, UnsupportedRequestModeCause {
    override val mode: String by raw

    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        mode: String,
        message: String,
        subject: SubjectDescriptor,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : super(
        CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE_ID,
        message, subject, cause, raw
    )
}

fun SessionLanguageConfiguration.createUnsupportedRequestModeCause(
    mode: String, subject: SubjectDescriptor, cause: Cause? = null
): UnsupportedRequestModeCause = UnsupportedRequestModeCauseImpl(
    mode, CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE, subject, cause
).apply {
    message = unsupportedRequestMode.format(raw)
}