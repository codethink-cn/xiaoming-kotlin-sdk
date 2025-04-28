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

package cn.codethink.xiaoming.util

class SegmentIdPatternImpl(
    elements: List<SegmentIdPatternElement>
) : SegmentIdPattern {
    override val elements: List<SegmentIdPatternElement>
    private val toStringCache by lazy { elements.joinToString(".") }

    init {
        require(elements.isNotEmpty()) { "Path ID pattern must not be empty" }
        require(elements.all {
            it is WildCardSegmentIdPatternElement || it is SingleSegmentIdPatternElement
        }) {
            "Path ID pattern must only contain single or wild card elements, but found $elements"
        }

        for (i in 1 until elements.size) {
            val previous = elements[i - 1]
            val current = elements[i]

            require(previous !is WildCardSegmentIdPatternElement || current !is WildCardSegmentIdPatternElement) {
                "Wild card elements cannot be adjacent, but found near index $previous and $current"
            }
        }

        this.elements = elements.toList()
    }

    override fun matches(id: SegmentId): Boolean {
        var elementIndex = 0
        var segmentIndex = 0

        while (elementIndex < elements.size && segmentIndex < id.size) {
            val element = elements[elementIndex]
            val segment = id[segmentIndex]

            when (element) {
                is SingleSegmentIdPatternElement -> {
                    if (element.matches(segment)) {
                        elementIndex++
                        segmentIndex++
                    } else {
                        return false
                    }
                }

                is WildCardSegmentIdPatternElement -> {
                    // 如果通配符元素是当前列表中的最后一个，则必然匹配成功。
                    val nextElement = if (elementIndex + 1 < elements.size) elements[elementIndex + 1] else return true
                    check(nextElement is SingleSegmentIdPatternElement) {
                        "The next element after a wild card must be a single element, but found $nextElement"
                    }

                    val minNextSegmentIndex = if (element.isOptional) segmentIndex else segmentIndex + 1
                    if (element.isGreedy) {
                        segmentIndex = id.size - 1
                        while (segmentIndex > minNextSegmentIndex) {
                            val nextSegment = id[segmentIndex]
                            if (nextElement.matches(nextSegment)) {
                                break
                            }
                            segmentIndex--
                        }
                        if (segmentIndex == minNextSegmentIndex && minNextSegmentIndex != id.size - 1) {
                            return false
                        }
                    } else {
                        segmentIndex = minNextSegmentIndex
                        while (segmentIndex < id.size) {
                            val nextSegment = id[segmentIndex]
                            if (nextElement.matches(nextSegment)) {
                                break
                            }
                            segmentIndex++
                        }
                        if (segmentIndex == id.size) {
                            return false
                        }
                    }

                    segmentIndex += 1
                    elementIndex += 2
                }

                else -> throwUnexpectedElementException(elementIndex)
            }
        }

        // 如果还剩下了一些元素，检查是否都是 isOptional = true 的通配符元素
        while (elementIndex < elements.size) {
            val element = elements[elementIndex]
            if (element is WildCardSegmentIdPatternElement && element.isOptional) {
                elementIndex++
            } else {
                return false
            }
        }

        // 如果还剩下了一些段落，检查最后一个元素是否是通配符元素，且能匹配剩余所有元素
        if (segmentIndex < id.size) {
            return when (elements.last()) {
                is SingleSegmentIdPatternElement -> false
                is WildCardSegmentIdPatternElement -> true
                else -> throwUnexpectedElementException(elements.size - 1)
            }
        }

        return true
    }

    private fun throwUnexpectedElementException(index: Int): Nothing {
        throw IllegalStateException("Unexpected element at index $index: ${elements[index]} in $this")
    }

    override fun toString(): String = toStringCache

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentIdPatternImpl

        return elements == other.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }
}
