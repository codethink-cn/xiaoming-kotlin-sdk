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

package cn.codethink.xiaoming.plugin

import cn.codethink.xiaoming.LocalPlatform
import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.ErrorPolicy
import cn.codethink.xiaoming.common.Expected
import cn.codethink.xiaoming.common.success

class PluginConflictResolverContext(
    val platform: LocalPlatform,
    val cause: Cause,
    val plugins: List<PluginToDetector>,
    val policy: ErrorPolicy
)

/**
 * Plugin ID conflicts, providing relations conflict resolver.
 *
 * @author Chuanwise
 */
interface PluginCoexistenceResolver {
    fun resolve(context: PluginConflictResolverContext): Expected<Iterable<Plugin>>
}

object NoOperationPluginCoexistenceResolver : PluginCoexistenceResolver {
    override fun resolve(context: PluginConflictResolverContext): Expected<Iterable<Plugin>> {
        return success(context.plugins.map { it.first })
    }
}
