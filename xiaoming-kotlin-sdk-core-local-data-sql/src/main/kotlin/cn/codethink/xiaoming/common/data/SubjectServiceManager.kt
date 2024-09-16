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

package cn.codethink.xiaoming.common.data

import cn.codethink.xiaoming.common.DefaultRegistration
import cn.codethink.xiaoming.common.DefaultStringMapRegistrations
import cn.codethink.xiaoming.common.SUBJECT_TYPE_PLUGIN
import cn.codethink.xiaoming.common.Subject
import cn.codethink.xiaoming.common.XiaomingSdkSubject
import cn.codethink.xiaoming.permission.data.sql.SqlLocalPlatformData

/**
 * Manage all [SubjectService]s.
 *
 * @author Chuanwise
 * @see SubjectService
 */
@Suppress("UNCHECKED_CAST")
class SubjectServiceManager(
    data: SqlLocalPlatformData
) : SubjectService<Subject> {
    private val subjects = SqlSubjects(data)
    private val registrations = DefaultStringMapRegistrations<SubjectService<*>>()

    init {
        register(SUBJECT_TYPE_PLUGIN, PluginSubjectService(data), XiaomingSdkSubject)
    }

    operator fun get(type: String) = registrations[type]
    fun getOrFail(type: String) = get(type)
        ?: throw NoSuchElementException("No subject service found for $type.")

    fun register(type: String, service: SubjectService<out Subject>, subject: Subject) {
        registrations.register(type, DefaultRegistration(service, subject))
    }

    fun unregisterByType(type: String) {
        registrations.unregisterByKey(type)
    }

    fun unregisterBySubject(subject: Subject) {
        registrations.unregisterBySubject(subject)
    }

    override fun getSubjectId(subject: Subject): Long? = get(subject.type)?.value?.let {
        (it as SubjectService<Subject>).getSubjectId(subject)
    }

    override fun getOrCreateSubjectId(subject: Subject): Long {
        val service = getOrFail(subject.type).value as SubjectService<Subject>
        return service.getOrCreateSubjectId(subject)
    }
}
