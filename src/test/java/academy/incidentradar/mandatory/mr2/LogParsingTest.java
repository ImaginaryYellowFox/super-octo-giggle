package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.support.CliRunner;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: разбор строки лога. */
@DisplayName("MR2. Парсер строки лога")
class LogParsingTest {

    private static final String REFERENCE_LINE = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] "
            + "\"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    private final LogParser parser = new LogParser();

    @Test
    @DisplayName("Эталонная строка разбирается на корректные поля")
    void referenceLineIsParsedCorrectly() {
        var expected = new LogRecord(
                "93.180.71.3",
                "-",
                OffsetDateTime.of(2015, 5, 17, 8, 5, 32, 0, ZoneOffset.UTC),
                "GET",
                "/downloads/product_1",
                "HTTP/1.1",
                304,
                0,
                "-",
                "Debian APT-HTTP/1.3");

        assertThat(parser.parse(REFERENCE_LINE)).contains(expected);
    }

    @Test
    @DisplayName("Метод, ресурс и протокол разбираются в три отдельных поля")
    void requestIsSplitIntoThreeFields() {

        var line = "10.0.0.1 - alice [01/Mar/2025:10:00:00 +0300] "
                + "\"POST /api/v1/payments?id=7 HTTP/2.0\" 201 15 \"https://bank.example/\" \"Mozilla/5.0 (Windows NT 10.0)\"";

        LogRecord record = parser.parse(line).orElseThrow();

        assertThat(record.method()).isEqualTo("POST");
        assertThat(record.resource()).isEqualTo("/api/v1/payments?id=7");
        assertThat(record.protocol()).isEqualTo("HTTP/2.0");
    }

    @Test
    @DisplayName("Битая строка пропускается с warning'ом, программа не падает")
    void malformedLineIsSkippedWithWarning() {

        var result = CliRunner.run(
                "--path", Path.of("samples", "logs", "malformed.log").toString());

        assertThat(result.exitCode())
                .as("Программа не должна падать на битых строках.%n%s", result)
                .isZero();

        long warnings = result.stderr()
                .lines()
                .filter(line -> line.contains("WARN") && line.contains("skipped malformed log line"))
                .count();
        assertThat(warnings)
                .as("По одному предупреждению на каждую из 6 битых строк.%n%s", result)
                .isEqualTo(6);

        assertThat(result.stdout()).as("Корректные записи обработаны").contains("Всего запросов: 7");
    }
}
