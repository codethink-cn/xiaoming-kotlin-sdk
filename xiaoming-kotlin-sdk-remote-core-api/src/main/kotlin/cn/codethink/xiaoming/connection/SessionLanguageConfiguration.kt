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

import cn.codethink.xiaoming.util.Template

/**
 * 语言配置。
 *
 * @author Chuanwise
 */
interface SessionLanguageConfiguration {
    val unsupportedSession: Template
    val sessionRequired: Template
    val sessionRejected: Template
    val sessionExisted: Template
    val unsupportedRequestMode: Template
    val actionError: Template
    val invalidSession: Template

    val unsupportedRequestAction: Template
    val internalActionHandlerError: Template
    val actionHandlerTimeout: Template
}