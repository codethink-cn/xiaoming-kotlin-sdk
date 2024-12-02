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

package cn.codethink.xiaoming.util

import cn.codethink.xiaoming.plugin.PluginRuntimeMeta
import cn.codethink.xiaoming.plugin.PluginState

// Protected by outer lock.
@InternalApi
interface MutablePluginRuntimeMeta : PluginRuntimeMeta {
    override var state: PluginState

    override var isEnabled: Boolean
    override var isEnabling: Boolean
    override var isEnablingErrored: Boolean

    override var isDisabled: Boolean
    override var isDisabling: Boolean
    override var isDisablingErrored: Boolean

    override var isLoaded: Boolean
    override var isLoading: Boolean
    override var isLoadingErrored: Boolean

    override var isUnloaded: Boolean
    override var isUnloading: Boolean
    override var isUnloadingErrored: Boolean

    override val provisions: MutableMap<NamespaceId, Version>
}