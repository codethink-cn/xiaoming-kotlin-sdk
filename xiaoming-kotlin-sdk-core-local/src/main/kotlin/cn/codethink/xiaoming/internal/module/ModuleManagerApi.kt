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

package cn.codethink.xiaoming.internal.module

import cn.codethink.xiaoming.common.Cause
import cn.codethink.xiaoming.common.DefaultMapRegistrations
import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.ModuleSubject
import cn.codethink.xiaoming.common.Registration
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.internal.LocalPlatformInternalApi
import cn.codethink.xiaoming.internal.LocalPlatformInternalState
import cn.codethink.xiaoming.internal.assertState
import kotlin.concurrent.write

/**
 * The manager of modules.
 *
 * @author Chuanwise
 * @see Module
 */
class ModuleManagerApi(
    private val internalApi: LocalPlatformInternalApi
) {
    val logger by internalApi::logger

    // Protected by `internalApi.lock`.
    private val registrations = DefaultMapRegistrations<ModuleSubject, Module>()
    val modules: Map<ModuleSubject, Registration<Module>>
        get() = registrations.toMap()

    /**
     * Try to install specified module.
     *
     * @param module the module to install.
     * @param subject the subject who is installing the module.
     * @throws IllegalStateException if platform is not starting.
     * @throws IllegalArgumentException if the module is already installed.
     */
    fun install(module: Module, cause: Cause, subject: Subject) = internalApi.lock.write {
        internalApi.assertState(LocalPlatformInternalState.STARTING) {
            "$subject tried to install module ${module.subject}, but the platform is not starting."
        }

        registrations[module.subject]?.let {
            throw IllegalArgumentException("Module ${module.subject} is already installed.")
        }
        module.onPlatformStart(ModuleContext(internalApi, subject, cause))
        registrations.register(module.subject, DefaultRegistration(module, subject))
    }

    /**
     * Try to uninstall specified module.
     *
     * @param moduleSubject the subject of the module to uninstall.
     * @param subject the subject who is uninstalling the module.
     * @throws IllegalStateException if platform is not stopping.
     * @throws IllegalArgumentException if the module is not installed.
     */
    fun uninstall(moduleSubject: ModuleSubject, cause: Cause, subject: Subject) = internalApi.lock.write {
        internalApi.assertState(LocalPlatformInternalState.STOPPING) {
            "$subject tried to uninstall module $moduleSubject, but the platform is not stopping."
        }
        registrations[moduleSubject]?.let {
            it.value.onPlatformStopped(ModuleContext(internalApi, subject, cause))
            registrations.unregisterByKey(moduleSubject)
        } ?: throw IllegalArgumentException(
            "$subject tried to uninstall module $moduleSubject, but it is not installed."
        )
    }

    /**
     * Try to uninstall specified module.
     *
     * @param module the module to uninstall.
     * @param subject the subject who is uninstalling the module.
     * @throws IllegalStateException if platform is not stopping.
     * @throws IllegalArgumentException if the module is not installed.
     */
    fun uninstall(module: Module, cause: Cause, subject: Subject) = uninstall(module.subject, cause, subject)
}