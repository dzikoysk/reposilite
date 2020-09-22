/*
 * Copyright (c) 2020 Dzikoysk
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

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class ReposiliteWriterTest {

    @Test
    void 'should call custom consumer' () {
        def reference = new AtomicReference<String>(null)

        ReposiliteWriter.getConsumers().put("key", { String value -> reference.set(value) } as Consumer<String>)
        Reposilite.getLogger().info("ReposiliteWriterTest message")

        assertTrue reference.get().contains("ReposiliteWriterTest message")
    }

    @Test
    void 'should not exceed cache limit' () {
        for (int index = 0; index < ReposiliteWriter.CACHE_SIZE; index++) {
            Reposilite.getLogger().info(Integer.toString(index))
        }
        assertEquals ReposiliteWriter.CACHE_SIZE, ReposiliteWriter.getCache().size()

        Reposilite.getLogger().info("above limit")
        assertEquals ReposiliteWriter.CACHE_SIZE, ReposiliteWriter.getCache().size()
    }

    @Test
    void 'should contain given messages'() {
        Reposilite.getLogger().info("test::contains")

        assertTrue ReposiliteWriter.contains("test::contains")
        assertFalse ReposiliteWriter.contains("diorite::release_date")
    }

}