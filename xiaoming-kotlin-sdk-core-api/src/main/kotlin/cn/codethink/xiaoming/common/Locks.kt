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

@file:JvmName("Locks")

package cn.codethink.xiaoming.common

import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Upgrade the read lock to write lock, and execute the action.
 * After the action is executed, downgrade the write lock to read lock.
 *
 * @param T the return type of the action.
 * @author Chuanwise
 */
fun <T> ReentrantReadWriteLock.upgrade(action: () -> T) {
    readLock().unlock()
    writeLock().lock()
    try {
        action()
    } finally {
        writeLock().unlock()
        readLock().lock()
    }
}