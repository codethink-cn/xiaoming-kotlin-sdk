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

import cn.codethink.xiaoming.data.PlatformData
import cn.codethink.xiaoming.exception.LocalExceptionManager
import io.github.oshai.kotlinlogging.KLogger

/**
 * 本地平台配置。
 *
 * @author Chuanwise
 */
interface LocalPlatformConfiguration : PlatformConfiguration {
    /**
     * 平台的日志。
     */
    val logger: KLogger

    /**
     * 平台数据源。
     */
    val data: PlatformData

    /**
     * 异常管理器。
     */
    val exceptionManager: LocalExceptionManager

    /**
     * 用于插件类加载隔离时，加载 `java.` 和 `cn.codethink.xiaoming.` 开头的运行平台类。
     */
    val systemClassLoader: ClassLoader
}