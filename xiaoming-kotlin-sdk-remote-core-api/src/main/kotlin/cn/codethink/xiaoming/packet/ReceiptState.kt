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

package cn.codethink.xiaoming.packet

/**
 * 请求数据包的回执状态。
 *
 * @author Chuanwise
 */
enum class ReceiptState {
    /**
     * 请求成功处理。
     */
    SUCCESS,

    /**
     * 请求无法执行，或执行中失败。
     */
    FAILURE,

    /**
     * 请求处理超时。
     */
    TIMEOUT
}