package academy.incidentradar.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.filter.DateRange;
import academy.incidentradar.source.LogSourceException;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IncidentRadar")
class IncidentRadarTest {

    private static final Path SAMPLES = Path.of("samples", "logs");
    private static final String ACCESS_LOGS = SAMPLES + File.separator + "access-*.log";

    private final IncidentRadar radar = new IncidentRadar();

    @Test
    @DisplayName("Битые строки пропускаются, а корректные считаются")
    void malformedLinesAreSkipped() {
        RadarResult result = radar.run(List.of(SAMPLES.resolve("malformed.log").toString()), DateRange.unbounded());

        assertThat(result.statistics().totalRequests()).isEqualTo(7);
        assertThat(result.skippedLines()).isEqualTo(6);
        assertThat(result.outOfRange()).isZero();
        assertThat(result.linesRead()).isEqualTo(13);
    }

    @Test
    @DisplayName("Фильтр по датам на сэмплах: --from 2025-03-02 оставляет второй день")
    void dateFilterOnSamples() {
        RadarResult result = radar.run(List.of(ACCESS_LOGS), new DateRange(LocalDate.of(2025, 3, 2), null));

        assertThat(result.files()).hasSize(2);
        assertThat(result.statistics().totalRequests()).isEqualTo(1500);
        assertThat(result.outOfRange()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Период вне логов: выдаётся не ошибка, а пустая статистика")
    void periodOutsideLogsGivesEmptyStatistics() {
        RadarResult result = radar.run(List.of(ACCESS_LOGS), new DateRange(LocalDate.of(2030, 1, 1), null));

        assertThat(result.statistics().totalRequests()).isZero();
        assertThat(result.outOfRange()).isEqualTo(3000);
        assertThat(result.statistics().responseSize().average()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Недоступный источник бросает LogSourceException")
    void missingSourceFails() {
        assertThatThrownBy(() ->
                        radar.run(List.of(SAMPLES.resolve("no-such-file.log").toString()), DateRange.unbounded()))
                .isInstanceOf(LogSourceException.class);
    }
}
