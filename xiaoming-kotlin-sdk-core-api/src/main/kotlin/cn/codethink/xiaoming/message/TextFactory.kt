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

@file:JvmName("TextFactory")

package cn.codethink.xiaoming.message

import cn.codethink.xiaoming.api.CoreApi
import cn.codethink.xiaoming.util.InternalApi

@OptIn(InternalApi::class)
@JvmName("createText")
fun Text(string: String): Text {
    return CoreApi.getInstance().createText(string)
}

@JvmSynthetic
fun String.toText(): Text {
    return Text(this)
}