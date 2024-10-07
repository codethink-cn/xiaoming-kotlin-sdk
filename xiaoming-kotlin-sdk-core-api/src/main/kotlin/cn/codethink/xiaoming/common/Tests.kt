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

import cn.codethink.xiaoming.io.data.EmptyRaw

const val SUBJECT_TYPE_TEST = "test"
const val CAUSE_TYPE_TEST = "test"

@InternalApi
data object TestSubjectDescriptor : SubjectDescriptor(SUBJECT_TYPE_TEST)

@InternalApi
data object TestCause : Cause(EmptyRaw) {
    override val type: String = CAUSE_TYPE_TEST
    override val cause: Cause? = null
    override val subject: SubjectDescriptor = TestSubjectDescriptor
}