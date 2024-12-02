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

package cn.codethink.xiaoming.event

/**
 * 事件状态。
 *
 * @author Chuanwise
 * @see EventContext
 */
enum class EventState {
    /**
     * 事件已经被分配，但还没有任何操作。
     */
    ALLOCATED,

    /**
     * 事件正在发布中。
     */
    PUBLISHING,

    /**
     * 事件已经被发布。
     */
    PUBLISHED,

    /**
     * 事件在发布时被中断。
     */
    INTERRUPTED
}