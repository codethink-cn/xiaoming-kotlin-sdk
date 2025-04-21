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

import java.lang.annotation.Inherited

/**
 * 标记一个仅供小明内部使用的 API。
 *
 * 这些 API 可能在任意时刻被更改，且不会发布任何预警。
 * 除非正在开发和小明本身，或底层插件，否则非常不建议使用这些 API。
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class InternalApi

/**
 * 标记一个 API 是为 Java 开发者使用而设计的 API。
 *
 * 一般有一定性能损失，或不符合 Kotlin 代码风格，请不要在 Kotlin 中使用。
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is designed to be used for Java developers. Kotlin developers should not use this."
)
annotation class JavaFriendlyApi

/**
 * 标记一个实验性 API，不具有稳定性，且可能在任意时刻更改。
 * 非常不建议在发行版中使用这些 API。
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalApi

/**
 * 标记一个 API 在使用上是稳定的，但只应该由小明内部实现。
 *
 * 开发者自行实现可能导致无法兼容未来版本，因为新的属性和函数可能在未经警告的前提下添加。
 * 且小明内部可能对其实现类有特殊处理而无法兼容。
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class NotStableForInheritance