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

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.util.CAUSE_TYPE_SESSION_REQUIRED
import cn.codethink.xiaoming.util.CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE
import cn.codethink.xiaoming.util.CAUSE_TYPE_UNSUPPORTED_SESSION
import cn.codethink.xiaoming.util.AbstractData
import cn.codethink.xiaoming.util.CAUSE_TYPE_ACTION_ERROR
import cn.codethink.xiaoming.util.CAUSE_TYPE_SESSION_INVALID
import cn.codethink.xiaoming.util.CAUSE_TYPE_SESSION_REJECTED
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.MutableStore
import cn.codethink.xiaoming.util.Template
import cn.codethink.xiaoming.util.property

class ConnectionLanguageConfigurationImpl @InternalApi constructor(
    raw: MutableStore
) : AbstractData(raw), SessionLanguageConfiguration {
    override val unsupportedSession: Template by raw.property(CAUSE_TYPE_UNSUPPORTED_SESSION)
    override val sessionRequired: Template by raw.property(CAUSE_TYPE_SESSION_REQUIRED)
    override val sessionRejected: Template by raw.property(CAUSE_TYPE_SESSION_REJECTED)
    override val unsupportedRequestMode: Template by raw.property(CAUSE_TYPE_UNSUPPORTED_REQUEST_MODE)
    override val actionError: Template by raw.property(CAUSE_TYPE_ACTION_ERROR)
    override val invalidSession: Template by raw.property(CAUSE_TYPE_SESSION_INVALID)

    override val unsupportedRequestAction: Template by raw.property()
    override val internalActionHandlerError: Template by raw.property()
    override val actionHandlerTimeout: Template by raw.property()
}