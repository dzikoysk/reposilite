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

package org.panda_lang.reposilite.error

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import org.panda_lang.reposilite.ReposiliteWriter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileStatic
class FailureServiceTest {

    @CompileStatic
    static class FailureServiceTestException extends Exception {
        FailureServiceTestException(String message) {
            super(message, null, false, false)
        }
    }

    @Test
    void 'should throw exception' () {
        def failureService = new FailureService()
        failureService.throwException('id', new FailureServiceTestException('FailureServiceTest'))

        Thread.sleep(10L) // make sure that tinylog service had a chance to process log
        assertTrue ReposiliteWriter.contains('FailureServiceTest')
    }

    @Test
    void 'should keep all exceptions' () {
        def failureService = new FailureService()
        failureService.throwException('1', new FailureServiceTestException('1'))
        failureService.throwException('2', new FailureServiceTestException('2'))

        assertEquals 2, failureService.getExceptions().size()
    }

}
