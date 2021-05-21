/*
 * Copyright (c) 2021 dzikoysk
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
package org.panda_lang.reposilite

/**
 * dirty af
 */
/*
class ReposiliteWriter(properties: Map<String?, String?>?) : AbstractFormatPatternWriter(properties) {
    override fun write(logEntry: LogEntry) {
        val message = render(logEntry)
        CACHE.add(message)
        CONSUMERS.forEach { (`object`: Any?, consumer: Consumer<String?>) -> consumer.accept(message) }
        print(message)
    }

    override fun flush() {}
    override fun close() {
        clear()
    }

    override fun getRequiredLogEntryValues(): Collection<LogEntryValue> {
        val logEntryValues = super.getRequiredLogEntryValues()
        logEntryValues.add(LEVEL)
        return logEntryValues
    }

    companion object {
        const val CACHE_SIZE = 100
        private val CACHE: Queue<String> = CircularFifoQueue(CACHE_SIZE)
        private val CONSUMERS: MutableMap<Any, Consumer<String?>> = ConcurrentHashMap()
        fun clear() {
            CACHE.clear()
            CONSUMERS.clear()
        }

        operator fun contains(message: String?): Boolean {
            return cache.stream()
                .filter { obj: String? -> Objects.nonNull(obj) }
                .anyMatch { line: String -> line.contains(message!!) }
        }

        @JvmStatic
        val cache: List<String>
            get() = ArrayList(CACHE)
        @JvmStatic
        val consumers: Map<Any, Consumer<String?>>
            get() = CONSUMERS
    }
}
 */