/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite

import org.assertj.core.api.Assertions.assertThat

internal fun assertCollectionsEquals(first: Collection<Any?>, second: Collection<Any?>) {
    if (first.size == second.size && first.containsAll(second) && second.containsAll(first)) {
        return
    }

    assertThat(first.sortedBy { it.toString() }).isEqualTo(second.sortedBy { it.toString() }) // pretty printing
}
