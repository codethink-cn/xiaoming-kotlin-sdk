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

package cn.codethink.xiaoming.action

import cn.codethink.xiaoming.util.Action
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.TypeMeta
import cn.codethink.xiaoming.util.SubjectDescriptor

data class ActionImpl<P, R>(
    override val id: Id,
    override val requestTypeMeta: TypeMeta<P>,
    override val receiptTypeMeta: TypeMeta<R>,
    override val subject: SubjectDescriptor
) : Action<P, R>

//inline fun <reified P, reified R> Action(
//    name: String,
//    subject: SubjectDescriptor
//): Action<P, R> = ActionImpl(
//    name = name,
//    requestPara = ActionValueImpl(
//        object : TypeReference<P>() {}.type,
//        optional = defaultOptional<P>(),
//        nullable = defaultNullable<P>(),
//        defaultValueFactory = defaultValueFactory()
//    ),
//    receiptData = ActionValueImpl(
//        object : TypeReference<R>() {}.type,
//        optional = defaultOptional<R>(),
//        nullable = defaultNullable<R>(),
//        defaultValueFactory = defaultValueFactory()
//    ),
//    subject = subject
//)