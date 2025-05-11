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

package cn.codethink.xiaoming.event

import cn.codethink.xiaoming.TestLocalPlatform
import cn.codethink.xiaoming.util.InternalApi
import cn.codethink.xiaoming.util.Operation
import cn.codethink.xiaoming.util.TestSubjectDescriptor
import cn.codethink.xiaoming.util.toNamespaceId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class LocalEventManagerTest {
    private val platform = TestLocalPlatform()

    object TestEvent : AbstractEvent() {
        override val description: String = "Just for test"
    }

    @OptIn(InternalApi::class)
    private val operation = Operation("LocalEventManagerTest", TestSubjectDescriptor)

    @Test
    fun testPublishEvent(): Unit = runBlocking {
        platform.eventManager.registerListener(
            id = "com.example:foo".toNamespaceId(),
            type = "test",
            operation = operation
        ) { e, _ ->
            println("Event received: $e")
        }

        platform.eventManager.publishEvent(TestEvent, operation)
    }
}