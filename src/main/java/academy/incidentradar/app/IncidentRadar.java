package academy.incidentradar.app;

import academy.incidentradar.filter.DateRange;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.UrlLogSource;
import academy.incidentradar.source.resolver.LogSourceResolver;
import academy.incidentradar.stats.StatisticsCollector;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IncidentRadar {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentRadar.class);

    private final LogParser parser = new LogParser();

    public RadarResult run(List<String> paths, DateRange dateRange) {
        try (HttpClient httpClient = UrlLogSource.newHttpClient()) {
            LogSourceResolver resolver = LogSourceResolver.standard(httpClient);

            List<LogSource> sources = paths.stream()
                    .flatMap(path -> resolver.resolve(path).stream())
                    .distinct()
                    .toList();

            LineProcessor processor = new LineProcessor(parser, dateRange);
            sources.forEach(processor::read);

            return processor.result(sources.stream().map(LogSource::name).toList());
        }
    }

    private static final class LineProcessor {
        private final LogParser parser;
        private final DateRange dateRange;
        private final StatisticsCollector statistics = new StatisticsCollector();
        private long outOfRange;
        private long skipped;
        private long lineNumber;

        LineProcessor(LogParser parser, DateRange dateRange) {
            this.parser = parser;
            this.dateRange = dateRange;
        }

        void read(LogSource source) {
            lineNumber = 0;
            source.forEachLine(line -> handle(source, line));
        }

        private void handle(LogSource source, String line) {
            lineNumber++;
            Optional<LogRecord> parsed = parser.parse(line);
            if (parsed.isEmpty()) {
                skipped++;
                LOG.warn("skipped malformed log line: {}:{}", source.name(), lineNumber);
                return;
            }

            LogRecord record = parsed.get();
            if (dateRange.test(record)) {
                statistics.accept(record);

            } else {
                outOfRange++;
            }
        }

        RadarResult result(List<String> files) {
            return new RadarResult(files, statistics.build(), outOfRange, skipped);
        }
    }
}
