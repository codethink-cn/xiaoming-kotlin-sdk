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

const val CAUSE_TYPE_ACTION_ERROR = "action.error"
private val CAUSE_TYPE_ACTION_ERROR_ID = CAUSE_TYPE_ACTION_ERROR.toStringId()

@JsonTypeName(CAUSE_TYPE_ACTION_ERROR)
class ActionErrorCauseImpl : AbstractStringIdStandardCause, ActionErrorCause {
    @InternalApi
    constructor(raw: Raw) : super(raw)

    @JvmOverloads
    constructor(
        message: String,
        subject: SubjectDescriptor,
        cause: Cause? = null,
        raw: Raw = MapRaw()
    ) : super(
        CAUSE_TYPE_ACTION_ERROR_ID,
        message, subject, cause, raw
    )
}

fun SessionLanguageConfiguration.createActionErrorCause(
    subject: SubjectDescriptor, cause: Cause? = null
) = ActionErrorCauseImpl(
    CAUSE_TYPE_ACTION_ERROR, subject, cause
).apply {
    message = actionError.format(raw)
}