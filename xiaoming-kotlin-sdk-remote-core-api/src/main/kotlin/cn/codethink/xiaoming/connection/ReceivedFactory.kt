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

@file:JvmName("ReceivedFactory")
@file:OptIn(InternalApi::class)

package cn.codethink.xiaoming.connection

import cn.codethink.xiaoming.api.RemoteCoreApi
import cn.codethink.xiaoming.util.InternalApi

fun <T> createReceived(origin: Any?, data: T): Received<T> = RemoteCoreApi.getInstance().createReceived(origin, data)