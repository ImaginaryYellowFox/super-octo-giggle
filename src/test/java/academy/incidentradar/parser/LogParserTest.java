package academy.incidentradar.parser;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("LogParser")
class LogParserTest {

    private static final String VALID_LINE = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] "
            + "\"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    private final LogParser parser = new LogParser();

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(
            strings = {
                "",
                "это вообще не лог",
                "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\"",
                "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /api/v1/cards HTTP/1.1\" двести 512 \"-\" \"curl/8.7.1\"",
                "- - - [17/May/2015:08:05:32 +0000] \"\" 200 0 \"-\" \"-\"",
                "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /\" 200 512 \"-\" \"curl/8.7.1\"",
                "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET / HTTP/1.1\" 200 512 \"-\" \"curl/8.7.1\" хвост",
                "93.180.71.3 - - [НЕ-ДАТА] \"GET /api/v1/cards HTTP/1.1\" 200 512 \"-\" \"curl/8.7.1\"",
                "93.180.71.3 - - [32/May/2015:08:05:32 +0000] \"GET / HTTP/1.1\" 200 512 \"-\" \"curl/8.7.1\"",
                "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET / HTTP/1.1\" 200 99999999999999999999 \"-\" \"curl/8.7.1\""
            })
    @DisplayName("Битая строка не разбирается")
    void malformedLinesAreRejected(String line) {
        assertThat(parser.parse(line)).isEmpty();
    }

    @Test
    @DisplayName("Размер ответа больше 2 ГБ разбирается без переполнения")
    void largeResponseSizeIsParsed() {
        var line =
                "10.0.0.1 - - [01/Mar/2025:10:00:00 +0000] \"GET /big.iso HTTP/1.1\" 200 5000000000 \"-\" \"curl/8.7.1\"";

        LogRecord record = parser.parse(line).orElseThrow();

        assertThat(record.bodyBytesSent()).isEqualTo(5_000_000_000L);
    }

    @Test
    @DisplayName("Разбор не зависит от локали системы")
    void parsingDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ru-RU"));

            assertThat(new LogParser().parse(VALID_LINE)).isPresent();
        } finally {
            Locale.setDefault(original);
        }
    }
}
