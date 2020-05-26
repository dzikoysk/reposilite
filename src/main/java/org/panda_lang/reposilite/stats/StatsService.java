package org.panda_lang.reposilite.stats;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class StatsService {

    private final StatsEntity entity;
    private final StatsStorage statsStorage;

    public StatsService() {
        this.entity = new StatsEntity();
        this.statsStorage = new StatsStorage();
    }

    public void save() throws IOException {
        statsStorage.saveStats(entity);
    }

    public void load() throws IOException {
        StatsEntity storedEntity = statsStorage.loadStats();
        entity.setRecords(storedEntity.getRecords());
    }

    public void record(String uri) {
        entity.getRecords().compute(uri, (key, count) -> (count == null) ? 1 : count + 1);
    }

    public Map<String, Integer> fetchStats(Predicate<Entry<String, Integer>> filter) {
        return entity.getRecords().entrySet().stream()
                .filter(filter)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,(oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public int sumRecords() {
        return entity.getRecords().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public int countRecords() {
        return entity.getRecords().size();
    }

}
