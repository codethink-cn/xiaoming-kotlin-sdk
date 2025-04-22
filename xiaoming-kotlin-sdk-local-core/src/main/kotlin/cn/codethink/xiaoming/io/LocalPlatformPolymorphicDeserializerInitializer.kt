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

package cn.codethink.xiaoming.io

import cn.codethink.xiaoming.permission.InheritancePermissionMatcher
import cn.codethink.xiaoming.permission.InheritancePermissionMatcherV1
import cn.codethink.xiaoming.permission.PermissionMatcher
import cn.codethink.xiaoming.permission.WildCardPermissionPattern
import cn.codethink.xiaoming.permission.WildCardPermissionPatternV1
import cn.codethink.xiaoming.serialization.CodecResolverInitializeContext
import cn.codethink.xiaoming.serialization.CodecResolverInitializer
import cn.codethink.xiaoming.serialization.registering

class LocalPlatformPolymorphicDeserializerInitializer : CodecResolverInitializer {
    override fun initialize(context: CodecResolverInitializeContext) {
        context.registering {
            type<PermissionMatcher> {
                type {
                    type<WildCardPermissionPattern>("wild_card") {
                        version {
                            hint<WildCardPermissionPatternV1>("1")
                        }
                    }

                    type<InheritancePermissionMatcher>("inheritance") {
                        version {
                            hint<InheritancePermissionMatcherV1>("1")
                        }
                    }
                }
            }
        }
    }
}