package academy.incidentradar.support;

import academy.incidentradar.model.LogRecord;
import java.time.OffsetDateTime;

public final class TestRecords {

    private static final OffsetDateTime DEFAULT_TIME = OffsetDateTime.parse("2025-03-01T12:00:00Z");

    private TestRecords() {}

    public static LogRecord request(String resource, int status, long size) {
        return new LogRecord(
                "127.0.0.1", "-", DEFAULT_TIME, "GET", resource, "HTTP/1.1", status, size, "-", "test-agent");
    }

    public static LogRecord requestAt(String time) {
        return new LogRecord(
                "127.0.0.1", "-", OffsetDateTime.parse(time), "GET", "/", "HTTP/1.1", 200, 0, "-", "test-agent");
    }
}
