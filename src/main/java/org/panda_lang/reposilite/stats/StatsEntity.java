package org.panda_lang.reposilite.stats;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class StatsEntity implements Serializable {

    private final Map<String, Integer> records = new HashMap<>(128);

    public void setRecords(Map<String, Integer> records) {
        this.records.putAll(records);
    }

    public Map<String, Integer> getRecords() {
        return records;
    }

}
