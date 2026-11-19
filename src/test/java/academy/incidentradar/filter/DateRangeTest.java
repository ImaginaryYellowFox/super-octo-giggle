package academy.incidentradar.filter;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.support.TestRecords;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DateRange")
class DateRangeTest {

    @Test
    @DisplayName("Дата записи берётся не в UTC, а в поясе из лога")
    void dateIsTakenInLogTimeZone() {
        LogRecord earlyMorning = TestRecords.requestAt("2025-03-02T01:00:00+03:00");
        assertThat(new DateRange(null, LocalDate.of(2025, 3, 1)).test(earlyMorning))
                .as("по местной дате запись относится ко 2 марта")
                .isFalse();
        assertThat(new DateRange(LocalDate.of(2025, 3, 2), null).test(earlyMorning))
                .isTrue();
    }
}
