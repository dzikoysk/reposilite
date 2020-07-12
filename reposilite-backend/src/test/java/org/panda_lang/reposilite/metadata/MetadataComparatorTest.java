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

package org.panda_lang.reposilite.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.panda_lang.utilities.commons.collection.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class MetadataComparatorTest {

    private static final MetadataComparator<Pair<String[], String>> METADATA_COMPARATOR = new MetadataComparator<>(Pair::getValue, Pair::getKey, pair -> false);

    private static final String[] STRINGS = {
            "2",
            "1",
            "a.12.00",
            "a-10.1",
            "a.2",
            "a-2-0",
            "a-2-0-SNAPSHOT",
            "a.2-classifier",
            "a.1.0",
            "a-1",
            "a",
            "b.1.0.0",
            "b"
    };

    @Test
    void testMetadataComparator() {
        List<String> strings = new ArrayList<>(Arrays.asList(STRINGS));
        Collections.shuffle(strings);

        String[] sorted = strings.stream()
                .map(string -> new Pair<>(string.split("[-.]"), string))
                .sorted(METADATA_COMPARATOR)
                .map(Pair::getValue)
                .toArray(String[]::new);

        System.out.println(Arrays.toString(STRINGS));
        System.out.println(Arrays.toString(sorted));
        Assertions.assertArrayEquals(STRINGS, sorted);
    }

}