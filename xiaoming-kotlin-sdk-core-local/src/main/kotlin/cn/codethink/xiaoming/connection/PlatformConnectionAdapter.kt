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

import cn.codethink.xiaoming.common.PlatformSubject
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PLATFORM
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.io.connection.Connection

/**
 * Adapt current connection as a platform.
 *
 * @author Chuanwise
 */
object PlatformConnectionAdapter : ConnectionAdapter {
    override fun adapt(api: LocalPlatformInternalApi, subject: Subject, connection: Connection): Boolean {
        if (subject !is PlatformSubject || subject.type != SUBJECT_TYPE_PLATFORM) {
            throw IllegalArgumentException("Unexpected subject: $subject.")
        }



        return true
    }
}