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

package cn.codethink.xiaoming.serialization

import cn.codethink.xiaoming.event.Event
import cn.codethink.xiaoming.plugin.PluginDisableEventImpl
import cn.codethink.xiaoming.plugin.PluginEnableEventImpl
import cn.codethink.xiaoming.plugin.PluginLoadEventImpl
import cn.codethink.xiaoming.plugin.PluginUnloadEventImpl
import cn.codethink.xiaoming.util.Cause
import cn.codethink.xiaoming.util.CauseImpl
import cn.codethink.xiaoming.util.Id
import cn.codethink.xiaoming.util.PluginDescriptor
import cn.codethink.xiaoming.util.PluginDescriptorImpl
import cn.codethink.xiaoming.util.SegmentIdPattern
import cn.codethink.xiaoming.util.SubjectDescriptor
import cn.codethink.xiaoming.util.Version
import cn.codethink.xiaoming.util.VersionPattern
import cn.codethink.xiaoming.util.toNamespaceId
import cn.codethink.xiaoming.util.toNumericalId
import cn.codethink.xiaoming.util.toSegmentIdPattern
import cn.codethink.xiaoming.util.toVersion
import cn.codethink.xiaoming.util.toVersionPattern

class CoreCodecResolverInitializer : CodecResolverInitializer {
    override fun initialize(context: CodecResolverInitializeContext) {
        context.registering {
            type<VersionPattern> {
                string { it.toVersionPattern() }
            }
            type<SubjectDescriptor> {
                type {
                    hint<PluginDescriptor>("plugin")
                }
            }

            type<Version> {
                string { it.toVersion() }
            }

            type<Cause> {
                fallback<CauseImpl>()
            }

            type<SegmentIdPattern> {
                string { it.toSegmentIdPattern() }
            }
            type<PluginDescriptor> {
                string(
                    serializer = { it.id.toString() },
                    deserializer = { PluginDescriptorImpl(it.toNamespaceId()) }
                )
            }

            type<Id> {
                int { it.toNumericalId() }
            }

            type<Event> {
                type {
                    hint<PluginLoadEventImpl>("plugin_load")
                    hint<PluginEnableEventImpl>("plugin_enable")
                    hint<PluginDisableEventImpl>("plugin_disable")
                    hint<PluginUnloadEventImpl>("plugin_unload")
                }
            }
        }
    }
}