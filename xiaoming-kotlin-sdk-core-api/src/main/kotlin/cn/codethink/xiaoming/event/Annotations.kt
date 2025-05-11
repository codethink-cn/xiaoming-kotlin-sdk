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

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.event.listener.ListenerPriority

/**
 * 在 [Event] 的实现类上使用，可自动注明事件的类型名。
 *
 * @property value 事件类型名
 * @author Chuanwise
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventType(val value: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listener(
    /**
     * 监听器 ID。
     */
    val id: String,

    /**
     * 事件类型，默认从函数参数获取。
     */
    val type: String = "",

    /**
     * 监听器优先级，默认 [ListenerPriority.DEFAULT]。
     */
    val priority: ListenerPriority = ListenerPriority.DEFAULT
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Before(vararg val value: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class After(vararg val value: String)