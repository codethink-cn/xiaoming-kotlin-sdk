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

import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.NamespaceId
import cn.codethink.xiaoming.util.Version
import java.util.concurrent.ConcurrentHashMap

@InternalApi
class LocalPluginRuntimeMetaImpl(
    override val remoteViews: MutableMap<Id, RemotePluginRuntimeMetaLocalView> = ConcurrentHashMap(),
    override var isLoaded: Boolean = false,
    override var isLoading: Boolean = false,
    override var isLoadingErrored: Boolean = false,
    override var isUnloaded: Boolean = false,
    override var isUnloading: Boolean = false,
    override var isUnloadingErrored: Boolean = false,
    override var isEnabled: Boolean = false,
    override var isEnabling: Boolean = false,
    override var isEnablingErrored: Boolean = false,
    override var isDisabled: Boolean = false,
    override var isDisabling: Boolean = false,
    override var isDisablingErrored: Boolean = false,
    override val provisions: MutableMap<NamespaceId, Version> = ConcurrentHashMap()
) : LocalPluginRuntimeMeta, MutablePluginRuntimeMeta {
    override val mode: PluginMode = PluginMode.LOCAL
}

@OptIn(InternalApi::class)
class RemotePluginRuntimeMetaRemoteViewImpl(
    override var isLoaded: Boolean = false,
    override var isLoading: Boolean = false,
    override var isLoadingErrored: Boolean = false,
    override var isUnloaded: Boolean = false,
    override var isUnloading: Boolean = false,
    override var isUnloadingErrored: Boolean = false,
    override var isEnabled: Boolean = false,
    override var isEnabling: Boolean = false,
    override var isEnablingErrored: Boolean = false,
    override var isDisabled: Boolean = false,
    override var isDisabling: Boolean = false,
    override var isDisablingErrored: Boolean = false,
    override val provisions: MutableMap<NamespaceId, Version> = ConcurrentHashMap()
) : RemotePluginRuntimeMetaRemoteView, MutablePluginRuntimeMeta {
    override val mode: PluginMode = PluginMode.REMOTE
}