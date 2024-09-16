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

@file:JvmName("Actions")

package cn.codethink.xiaoming.io.action

import cn.codethink.xiaoming.common.PUBLISH_EVENT_REQUEST_ACTION
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingProtocolSubject
import cn.codethink.xiaoming.common.defaultNullable
import cn.codethink.xiaoming.common.defaultOptional
import cn.codethink.xiaoming.io.data.Raw
import com.fasterxml.jackson.core.type.TypeReference
import java.lang.reflect.Type

/**
 * [Raw] needed properties.
 *
 * @author Chuanwise
 */
data class ActionValue<T>(
    val type: Type,
    val optional: Boolean,
    val nullable: Boolean,
    val defaultValue: T? = null
)

/**
 * Action, used to sending and handing request packets.
 *
 * @author Chuanwise
 */
data class Action<P, R>(
    val name: String,
    val requestArgument: ActionValue<P>,
    val receiptData: ActionValue<R>,
    val subject: Subject
)

inline fun <reified P, reified R> Action(
    name: String,
    subject: Subject
): Action<P, R> = Action(
    name = name,
    requestArgument = ActionValue(
        object : TypeReference<P>() {}.type,
        optional = defaultOptional<P>(),
        nullable = defaultNullable<P>()
    ),
    receiptData = ActionValue(
        object : TypeReference<R>() {}.type,
        optional = defaultOptional<R>(),
        nullable = defaultNullable<R>()
    ),
    subject = subject
)

inline fun <reified P, reified R> StandardAction(
    name: String
) = Action<P, R>(name, XiaomingProtocolSubject)


val PUBLISH_EVENT = StandardAction<PublishEventRequestPara, PublishEventReceiptData>(PUBLISH_EVENT_REQUEST_ACTION)
