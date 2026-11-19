package academy.incidentradar.stats;

import academy.incidentradar.model.LogRecord;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class StatisticsCollector implements Consumer<LogRecord> {

    private static final int TOP_RESOURCES_LIMIT = 10;
    private static final int RESPONSE_SIZE_PERCENTILE = 95;

    private final Map<Integer, Long> responseCodes = new HashMap<>();
    private final Map<String, Long> resourceCounts = new HashMap<>();
    private final SizeHistogram responseSizes = new SizeHistogram();
    private long totalRequests;

    @Override
    public void accept(LogRecord record) {
        totalRequests++;
        responseCodes.merge(record.status(), 1L, Long::sum);
        resourceCounts.merge(record.resource(), 1L, Long::sum);
        responseSizes.add(record.bodyBytesSent());
    }

    public Statistics build() {
        List<ResourceCount> topResources = resourceCounts.entrySet().stream()
                .map(entry -> new ResourceCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ResourceCount::count)
                        .reversed()
                        .thenComparing(ResourceCount::resource))
                .limit(TOP_RESOURCES_LIMIT)
                .toList();
        ResponseSizeStatistics responseSize = new ResponseSizeStatistics(
                responseSizes.average(), responseSizes.max(), responseSizes.percentile(RESPONSE_SIZE_PERCENTILE));
        return new Statistics(totalRequests, responseCodes, topResources, responseSize);
    }
}
