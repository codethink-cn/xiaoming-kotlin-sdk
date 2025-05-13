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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.withSuffix
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KLoggingEventBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import io.github.oshai.kotlinlogging.Marker

@OptIn(InternalApi::class)
class PluginLogger(
    private val id: NamespaceId,
    private val plugin: AvailablePlugin
) : KLogger {
    private val backend = KotlinLogging.logger("${Plugin::class}")
    override val name: String get() = backend.name

    private val debugDisabledLogger = backend.withSuffix("[$id]")

    private inner class DebugLogger : KLogger {
        private val logger = backend.withSuffix("[$id, debugging]")
        override val name: String get() = backend.name

        override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
            val finalLevel = if (level == Level.DEBUG) Level.INFO else level
            logger.at(finalLevel, marker, block)
        }

        override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean {
            return if (level == Level.DEBUG) true else logger.isLoggingEnabledFor(level, marker)
        }
    }

    private val debugEnabledLogger = DebugLogger()

    private val logger: KLogger get() = if (plugin.debug) debugEnabledLogger else debugDisabledLogger

    override fun at(level: Level, marker: Marker?, block: KLoggingEventBuilder.() -> Unit) {
        logger.at(level, marker, block)
    }

    override fun isLoggingEnabledFor(level: Level, marker: Marker?): Boolean {
        return logger.isLoggingEnabledFor(level, marker)
    }
}