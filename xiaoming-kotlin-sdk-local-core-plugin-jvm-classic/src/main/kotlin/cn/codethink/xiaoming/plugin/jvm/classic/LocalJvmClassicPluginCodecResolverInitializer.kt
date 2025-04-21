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

package cn.codethink.xiaoming.plugin.jvm.classic

import cn.codethink.xiaoming.plugin.PluginMeta
import cn.codethink.xiaoming.util.FIELD_TYPE
import cn.codethink.xiaoming.serialization.CodecResolverInitializer
import cn.codethink.xiaoming.serialization.name
import cn.codethink.xiaoming.serialization.names

/**
 * @see CodecResolverInitializer
 * @author Chuanwise
 */
class LocalJvmClassicPluginCodecResolverInitializer : CodecResolverInitializer {
    override fun initialize(context: SerializationHandlerManagerInitializeContext) {
        context.deserializers.registering(context.operation) {
            names<PluginMeta>(FIELD_TYPE) {
                name<LocalJvmClassicPluginMeta>()
            }
        }
    }
}