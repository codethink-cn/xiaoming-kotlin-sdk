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

import cn.codethink.xiaoming.plugin.Plugin

/**
 * 上下文感知事件：事件发布前，可根据当前环境填充事件的上下文。
 *
 * 由于事件可能被传播到其他进程，其内容只能是纯数据内容，这会为监听器带来许多麻烦。
 * 例如，插件相关事件可能携带 `pluginId` 属性，所有监听器都要先由此查找到插件对象再做操作。
 *
 * 在事件发布前，若事件实现 [ContextAwareEvent] 接口，框架会传入事件上下文以便填充相关字段。
 * 这样可以让远程监听器像在本地那样正常使用复杂对象，例如 [Plugin]。
 *
 * @param E 事件类型
 * @author Chuanwise
 */
interface ContextAwareEvent<E : ContextAwareEvent<E>> : Event {
    fun applyEventContext(context: EventContext<E>)
}