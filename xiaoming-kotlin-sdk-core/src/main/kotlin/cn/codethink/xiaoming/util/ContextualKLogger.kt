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

package cn.codethink.xiaoming.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker
import io.github.oshai.kotlinlogging.withLoggingContext

@InternalApi
class ContextualKLogger(
    private val logger: KLogger,
    private val context: Pair<String, String?>,
    private val restorePrevious: Boolean = true
) : KLogger {
    override val name: String = logger.name

    override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
        withLoggingContext(context, restorePrevious) {
            logger.at(level, marker, block)
        }
    }

    override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean {
        return logger.isLoggingEnabledFor(level, marker)
    }
}

@InternalApi
fun KLogger.withContext(
    key: String,
    value: String?,
    restorePrevious: Boolean = true
): KLogger {
    return ContextualKLogger(this, key to value, restorePrevious)
}

@InternalApi
fun KLogger.withSuffix(suffix: String?, restorePrevious: Boolean = true): KLogger {
    return ContextualKLogger(this, "suffix" to suffix, restorePrevious)
}
