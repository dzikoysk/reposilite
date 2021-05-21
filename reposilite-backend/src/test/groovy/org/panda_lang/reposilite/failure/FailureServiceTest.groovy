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

package org.panda_lang.reposilite.failure

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteWriter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class FailureServiceTest {

    @CompileStatic
    static class FailureServiceTestException extends Exception {
        FailureServiceTestException(String message, boolean stacktrace) {
            super(message, null, false, stacktrace)
        }
    }

    @Test
    void 'should throw exception' () {
        def failureService = new FailureService()
        failureService.throwException('id', new FailureServiceTestException('FailureServiceTest', true))

        Thread.sleep(10L) // make sure that tinylog service had a chance to process log
        assertTrue ReposiliteWriter.contains('FailureServiceTest')
    }

    @Test
    void 'should keep all exceptions' () {
        def failureService = new FailureService()
        failureService.throwException('id1', new FailureServiceTestException('message1', true))
        failureService.throwException('id2', new FailureServiceTestException('message2', false))

        assertEquals 2, failureService.getFailures().size()
    }

}
