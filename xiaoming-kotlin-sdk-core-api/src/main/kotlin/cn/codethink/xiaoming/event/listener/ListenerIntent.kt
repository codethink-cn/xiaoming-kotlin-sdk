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

package cn.codethink.xiaoming.event.listener

/**
 * 监听器意图：监听器用于声明其是否能读写事件。
 *
 * 发布事件时，事件管理器会检查 [EventPolicy.mutable]，并根据监听器意图决定需要回调哪些监听器。
 *
 * @author Chuanwise
 */
enum class ListenerIntent {
    /**
     * 监听器总是会收到事件，但它只能读事件。
     */
    READ,

    /**
     * 监听器总是会收到事件，当事件只读时，监听器不会修改事件；否则，监听器**可能**修改事件。
     */
    WRITE_OPTIONAL,

    /**
     * 监听器只能收到可写事件，无法收到只读事件，因为它一定会写事件。
     */
    WRITE_REQUIRED
}