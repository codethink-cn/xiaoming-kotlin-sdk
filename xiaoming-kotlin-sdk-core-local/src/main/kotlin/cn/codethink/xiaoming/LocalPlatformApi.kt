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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.ProtocolLanguageConfiguration
import cn.codethink.xiaoming.io.action.EventSnapshot

class LocalPlatformApi(
    val platformInternalApi: LocalPlatformInternalApi,
    override val language: ProtocolLanguageConfiguration
) : PlatformApi {

    fun start(cause: Cause, subject: Subject) {

    }

    override fun publishEvent(
        type: String,
        event: Event,
        mutable: Boolean,
        timeout: Long?,
        cause: Cause?
    ): List<EventSnapshot> {
        TODO("Not yet implemented")
    }
}