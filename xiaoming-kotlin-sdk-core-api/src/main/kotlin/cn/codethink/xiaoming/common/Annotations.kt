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

package cn.codethink.xiaoming.common

import java.lang.annotation.Inherited

/**
 * Mark the API is internal, which means that the API is not stable
 * and may be changed WITHOUT any warnings in the future.
 *
 * If the marked API is an open class, interface or open function,
 * it means all its implementations are internal.
 *
 * Notice that It's highly not recommended to use unless you are the
 * developer of the SDK.
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class InternalApi

/**
 * Mark the API is Java-friendly, which means that the API is designed
 * to be used in Java, Kotlin developers should not use it.
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    message = "This API is designed to be used for Java developers. " +
            "Kotlin developers should use another one provided in annotation field `replacement`.",
    level = RequiresOptIn.Level.ERROR
)
annotation class JavaFriendlyApi(val replacement: String)

/**
 * Mark the API is experimental, which means that the API is not stable
 * and may be changed in the future.
 *
 * If the marked API is an open class, interface or open function,
 * it means all its implementations are experimental.
 *
 * Notice that It's highly not recommended to use in release versions.
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalApi

/**
 * Mark the API is not stable, which means that the API is not stable
 * and may be changed in the future.
 *
 * If the marked API is an open class, interface or open function,
 * it means all its implementations are not stable.
 *
 * Notice that It's highly not recommended to inherit or construct them.
 *
 * @author Chuanwise
 */
@Inherited
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class NotStable