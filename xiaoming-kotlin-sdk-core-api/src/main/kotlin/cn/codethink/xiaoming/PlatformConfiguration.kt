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

package cn.codethink.xiaoming

import cn.codethink.xiaoming.serialization.SerializationManager
import cn.codethink.xiaoming.util.SubjectDescriptor
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.Job
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * 平台配置。
 *
 * @author Chuanwise
 */
interface PlatformConfiguration {
    /**
     * 平台的日志。
     */
    val logger: KLogger

    /**
     * 平台的地区设置，决定语言等信息。
     */
    val locale: Locale

    /**
     * 表示平台的主体描述符。
     */
    val descriptor: SubjectDescriptor

    /**
     * 平台协程任务的父级。
     */
    val parentJob: Job?

    /**
     * 平台协程上下文的父级。
     */
    val parentCoroutineContext: CoroutineContext

    /**
     * 平台的序列化管理器。
     */
    val serializationManager: SerializationManager
}