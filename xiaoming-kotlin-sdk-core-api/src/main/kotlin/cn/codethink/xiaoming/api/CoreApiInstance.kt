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

package cn.codethink.xiaoming.api

import cn.codethink.xiaoming.util.InternalApi
import java.util.ServiceLoader

/**
 * 用于存储 [CoreApi] 实例的容器。
 *
 * @author Chuanwise
 * @see CoreApi
 */
@InternalApi
class CoreApiInstance {
    companion object {
        /**
         * 全局唯一 [CoreApi] 实例。
         *
         * 它不是线程安全的，也不必线程安全，因为 [CoreApi] 实例之间是等价的。
         */
        @JvmStatic
        private var coreApi: CoreApi? = null

        /**
         * 获取 [CoreApi] 实例。若无实例，则会尝试加载第一个实现。
         */
        @JvmStatic
        fun get(): CoreApi {
            var api = coreApi
            if (api != null) {
                return api
            }

            val coreApiClass = CoreApi::class.java
            val apiClassName = coreApiClass.name

            val serviceLoader = ServiceLoader.load(coreApiClass)
            val iterator = serviceLoader.iterator()
            if (!iterator.hasNext()) {
                throw NoSuchElementException(
                    "No implementation of '$apiClassName' found. " +
                            "Make sure module 'xiaoming-kotlin-sdk-core' is included, or " +
                            "set the implementation manually by calling 'CoreApiInstance.set(api: Api)'."
                )
            }

            api = iterator.next()
            this.coreApi = api

            return api
        }

        /**
         * 设置 [CoreApi] 实例。
         */
        @JvmStatic
        fun set(coreApi: CoreApi) {
            this.coreApi = coreApi
        }
    }
}