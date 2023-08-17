/*
 * Copyright (C) 2010 The Libphonenumber Authors
 * Copyright (C) 2022 Michael Rozumyanskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.michaelrocks.libphonenumber.kotlin.internal


internal typealias Weigher<Key, Value> = (Key, Value?) -> Int

/**
 * LRU Cache for compiled regular expressions used by the libphonenumbers libary.
 *
 * @author Shaopeng Jia
 */
class RegexCache(size: Int) {
    private val cache: LruCache<String, Regex?>

    init {
        cache = LruCache(size)
    }

    fun getRegexForPattern(pattern: String): Regex {
        var regex = cache[pattern]
        if (regex == null) {
            regex = Regex(pattern)
            cache[pattern] = regex
        }
        return regex
    }

    // @VisibleForTesting
    fun containsRegex(regex: String): Boolean {
        return cache.keys().contains(regex)
    }


    /**
     * Multiplatform LRU cache implementation.
     * https://github.com/apollographql/apollo-kotlin/blob/main/apollo-normalized-cache-api/src/commonMain/kotlin/com/apollographql/apollo3/cache/normalized/api/internal/LruCache.kt
     *
     * Implementation is based on usage of [LinkedHashMap] as a container for the cache and custom
     * double linked queue to track LRU property.
     *
     * [maxSize] - maximum size of the cache, can be anything bytes, number of entries etc. By default is number o entries.
     * [weigher] - to be called to calculate the estimated size (weight) of the cache entry defined by its [Key] and [Value].
     *             By default it returns 1.
     *
     * Cache trim performed only on new entry insertion.
     */
    internal class LruCache<Key, Value>(
        private val maxSize: Int,
        private val weigher: Weigher<Key, Value> = { _, _ -> 1 },
    ) {
        private val cache = LinkedHashMap<Key, Node<Key, Value>>(0, 0.75f)
        private var headNode: Node<Key, Value>? = null
        private var tailNode: Node<Key, Value>? = null
        private var size: Int = 0

        operator fun get(key: Key): Value? {
            val node = cache[key]
            if (node != null) {
                moveNodeToHead(node)
            }
            return node?.value
        }

        operator fun set(key: Key, value: Value) {
            val node = cache[key]
            if (node == null) {
                cache[key] = addNode(key, value)
            } else {
                node.value = value
                moveNodeToHead(node)
            }

            trim()
        }

        fun remove(key: Key): Value? {
            return removeUnsafe(key)
        }

        fun keys() = cache.keys

        private fun removeUnsafe(key: Key): Value? {
            val nodeToRemove = cache.remove(key)
            val value = nodeToRemove?.value
            if (nodeToRemove != null) {
                unlinkNode(nodeToRemove)
            }
            return value
        }

        fun remove(keys: Collection<Key>) {
            keys.forEach { key -> removeUnsafe(key) }
        }

        fun clear() {
            cache.clear()
            headNode = null
            tailNode = null
            size = 0
        }

        fun size(): Int {
            return size
        }

        fun dump(): Map<Key, Value> {
            return cache.mapValues { (_, value) ->
                @Suppress("UNCHECKED_CAST")
                value.value as Value
            }
        }

        private fun trim() {
            var nodeToRemove = tailNode
            while (nodeToRemove != null && size > maxSize) {
                cache.remove(nodeToRemove.key)
                unlinkNode(nodeToRemove)
                nodeToRemove = tailNode
            }
        }

        private fun addNode(key: Key, value: Value?): Node<Key, Value> {
            val node = Node(
                key = key,
                value = value,
                next = headNode,
                prev = null,
            )

            headNode = node

            if (node.next == null) {
                tailNode = headNode
            } else {
                node.next?.prev = headNode
            }

            size += weigher(key, value)

            return node
        }

        private fun moveNodeToHead(node: Node<Key, Value>) {
            if (node.prev == null) {
                return
            }

            node.prev?.next = node.next

            if (node.next == null) {
                tailNode = node.prev
            } else {
                node.next?.prev = node.prev
            }

            node.next = headNode
            node.prev = null

            headNode?.prev = node
            headNode = node
        }

        private fun unlinkNode(node: Node<Key, Value>) {
            if (node.prev == null) {
                this.headNode = node.next
            } else {
                node.prev?.next = node.next
            }

            if (node.next == null) {
                this.tailNode = node.prev
            } else {
                node.next?.prev = node.prev
            }

            size -= weigher(node.key!!, node.value)

            node.key = null
            node.value = null
            node.next = null
            node.prev = null
        }

        private class Node<Key, Value>(
            var key: Key?,
            var value: Value?,
            var next: Node<Key, Value>?,
            var prev: Node<Key, Value>?,
        )
    }
}
