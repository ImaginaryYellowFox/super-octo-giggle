package academy.incidentradar.app;

import academy.incidentradar.stats.Statistics;
import java.util.List;

public record RadarResult(List<String> files, Statistics statistics, long outOfRange, long skippedLines) {

    public RadarResult {
        files = List.copyOf(files);
    }

    public long linesRead() {
        return statistics.totalRequests() + outOfRange + skippedLines;
    }
}
