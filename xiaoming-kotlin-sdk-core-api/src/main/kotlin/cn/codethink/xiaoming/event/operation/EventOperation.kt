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

package cn.codethink.xiaoming.event.operation

import cn.codethink.xiaoming.event.listener.ListenerDescriptor
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.InternalImplementedApi
import cn.codethink.xiaoming.util.Time

/**
 * 对事件进行的操作。
 *
 * 平台在调用每个监听器处理事件的前后，对比事件的差异并将其记录，得到事件的修改历史表，
 * 以便后续监听器使用和方便调试。
 *
 * @author Chuanwise
 */
@InternalImplementedApi
interface EventOperation {
    val time: Time
    val cause: Cause
    val listener: ListenerDescriptor
}