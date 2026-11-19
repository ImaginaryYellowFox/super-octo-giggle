package academy.incidentradar.stats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record Statistics(
        long totalRequests,
        Map<Integer, Long> responseCodes,
        List<ResourceCount> topResources,
        ResponseSizeStatistics responseSize) {

    public Statistics {
        responseCodes = Collections.unmodifiableMap(new TreeMap<>(responseCodes));
        topResources = List.copyOf(topResources);
    }
}
