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

package com.reposilite.storage

import com.reposilite.storage.specification.VersionComparatorSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VersionComparatorTest : VersionComparatorSpecification() {

    @Test
    fun `should sort semantic versions in ascending order`() {
        // given: an unordered list of versions
        val versions = listOf(
            "1.2.3-SNAPSHOT",
            "1.2",
            "0.5",
            "1.0",
            "1.1.5-SNAPSHOT",
            "word",
            "1.1.4",
            "1.1",
            "1.1.4-SNAPSHOT"
        )

        // when: an unordered list is sorted
        val sortedResult = versions.sortedWith(versionComparator)

        // then: sorted list matches expected rules
        assertThat(sortedResult).isEqualTo(
            listOf(
                "word",
                "0.5",
                "1.0",
                "1.1",
                "1.1.4-SNAPSHOT",
                "1.1.4",
                "1.1.5-SNAPSHOT",
                "1.2",
                "1.2.3-SNAPSHOT"
            )
        )
    }

    @Test
    fun `should sort double-digit style minor versions in ascending order`() {
        // given: an unordered list of versions
        val versions = listOf(
            "0.12",
            "1.00",
            "1.05",
            "1.12-SNAPSHOT",
            "1.12",
            "word",
            "1.20",
            "1.20.5-SNAPSHOT"
        )

        // when: an unordered list is sorted
        val sortedResult = versions.sortedWith(versionComparator)

        // then: sorted list matches expected rules
        assertThat(sortedResult).isEqualTo(
            listOf(
                "word",
                "0.12",
                "1.00",
                "1.05",
                "1.12-SNAPSHOT",
                "1.12",
                "1.20",
                "1.20.5-SNAPSHOT"
            )
        )
    }

    // GH-2421: Documents known limitation where timestamp suffixes cause incorrect ordering
    // when versions have different segment counts (e.g. 1.21-timestamp vs 1.21.2-timestamp).
    // The comparator treats all separators (.-_) equally, so it compares the timestamp against the patch version.
    // Users can work around this by using ?sorted=false to preserve raw metadata order.
    // TODO: Address in 4.x
    @Test
    fun `should misorder versions with timestamp suffixes across different segment depths`() {
        // given: versions where timestamps cause segment misalignment (GH-2421)
        val versions = listOf(
            "1.21-20240613.152323",
            "1.21.2-20241022.151510",
            "1.21.1-20240808.144430"
        )

        // when: sorted using the version comparator
        val sortedResult = versions.sortedWith(versionComparator)

        // then: 1.21 is incorrectly placed after 1.21.2 because timestamp 20240613 > patch 2
        assertThat(sortedResult).isEqualTo(
            listOf(
                "1.21.1-20240808.144430",
                "1.21.2-20241022.151510",
                "1.21-20240613.152323"
            )
        )
    }

    @Test
    fun `should sort mixed contents in ascending order`() {
        // given: an unordered list of versions
        val versions = listOf(
            "1_0_1",
            "1.1_5_early_access",
            "1.0.2",
            "pre-1.12.5",
            "pre-1.12.5-SNAPSHOT",
            "1.1.pre.6",
            "2.6-SNAPSHOT",
            "1.1_7",
            "1.1_5"
        )

        // when: an unordered list is sorted
        val sortedResult = versions.sortedWith(versionComparator)

        // then: sorted list matches expected rules
        assertThat(sortedResult).isEqualTo(
            listOf(
                "pre-1.12.5-SNAPSHOT",
                "pre-1.12.5",
                "1_0_1",
                "1.0.2",
                "1.1.pre.6",
                "1.1_5_early_access",
                "1.1_5",
                "1.1_7",
                "2.6-SNAPSHOT"
            )
        )
    }

}
