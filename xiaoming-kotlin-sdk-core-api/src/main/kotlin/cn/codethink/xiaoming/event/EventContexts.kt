/*
 * Copyright 2025 CodeThink Technologies and contributors.
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

@file:JvmName("EventContexts")

package cn.codethink.xiaoming.event

val EventContext<*>.isPublishing: Boolean
    get() = state == EventState.PUBLISHING

val EventContext<*>.isPublished: Boolean
    get() = state == EventState.PUBLISHED

val EventContext<*>.isInterrupted: Boolean
    get() = state == EventState.INTERRUPTED

val EventContext<*>.isPublishedOrInterrupted: Boolean
    get() = isPublished || isInterrupted
