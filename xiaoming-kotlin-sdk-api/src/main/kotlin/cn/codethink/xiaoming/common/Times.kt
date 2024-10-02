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

@file:JvmName("Times")

package cn.codethink.xiaoming.common

import io.github.oshai.kotlinlogging.KLogger

/**
 * Get the current time in seconds.
 *
 * @author Chuanwise
 */
val currentTimeSeconds: Long
    get() = System.currentTimeMillis() / 1000

val currentTimeMillis: Long
    get() = System.currentTimeMillis()

@InternalApi
inline fun <reified T> withDurationLogging(
    logger: KLogger,
    description: String,
    crossinline block: () -> T
): T {
    logger.debug { "$description." }

    var durationTimeMillis = currentTimeMillis
    try {
        val result = block()
        durationTimeMillis = currentTimeMillis - durationTimeMillis

        logger.debug { "$description in ${durationTimeMillis}ms." }
        return result
    } catch (e: Exception) {
        logger.warn { "$description quit after ${durationTimeMillis}ms." }
        throw e
    }
}