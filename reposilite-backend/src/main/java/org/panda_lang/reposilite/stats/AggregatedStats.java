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

package org.panda_lang.reposilite.stats;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

final class AggregatedStats {

    private final StatsEntity aggregatedStats;

    AggregatedStats(StatsEntity aggregatedStats) {
        this.aggregatedStats = aggregatedStats;
    }

    @SafeVarargs
    public final Map<String, Integer> fetchStats(BiPredicate<String, Integer>... filters) {
        return aggregatedStats.getRecords().entrySet().stream()
                .filter(entry -> {
                    for (BiPredicate<String, Integer> filter : filters) {
                        if (!filter.test(entry.getKey(), entry.getValue())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public int countRecords() {
        return aggregatedStats.getRecords().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int countUniqueRecords() {
        return aggregatedStats.getRecords().size();
    }

    StatsEntity getAggregatedStatsEntity() {
        return aggregatedStats;
    }

}
