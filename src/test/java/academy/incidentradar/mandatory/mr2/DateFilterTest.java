package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.filter.DateRange;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.support.TestRecords;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: фильтрация по датам. */
@DisplayName("MR2. Фильтр по датам")
class DateFilterTest {

    private static final LocalDate FROM = LocalDate.of(2025, 3, 1);
    private static final LocalDate TO = LocalDate.of(2025, 3, 2);

    private static final LogRecord DAY_BEFORE_END = TestRecords.requestAt("2025-02-28T23:59:59Z");
    private static final LogRecord FROM_DAY_START = TestRecords.requestAt("2025-03-01T00:00:00Z");
    private static final LogRecord TO_DAY_END = TestRecords.requestAt("2025-03-02T23:59:59Z");
    private static final LogRecord DAY_AFTER_START = TestRecords.requestAt("2025-03-03T00:00:00Z");

    private static final List<LogRecord> RECORDS = List.of(DAY_BEFORE_END, FROM_DAY_START, TO_DAY_END, DAY_AFTER_START);

    private static List<LogRecord> filter(DateRange range) {
        return RECORDS.stream().filter(range).toList();
    }

    @Test
    @DisplayName("Без --from и --to обрабатываются все записи")
    void noFilterKeepsEverything() {

        assertThat(filter(DateRange.unbounded())).containsExactlyElementsOf(RECORDS);
    }

    @Test
    @DisplayName("Только --from: открытая правая граница")
    void onlyFromIsOpenEnded() {

        assertThat(filter(new DateRange(FROM, null))).containsExactly(FROM_DAY_START, TO_DAY_END, DAY_AFTER_START);
    }

    @Test
    @DisplayName("Только --to: открытая левая граница")
    void onlyToIsOpenEnded() {

        assertThat(filter(new DateRange(null, TO))).containsExactly(DAY_BEFORE_END, FROM_DAY_START, TO_DAY_END);
    }

    @Test
    @DisplayName("Обе границы: период включительно")
    void bothBoundsAreInclusive() {
        assertThat(filter(new DateRange(FROM, TO))).containsExactly(FROM_DAY_START, TO_DAY_END);
    }
}
