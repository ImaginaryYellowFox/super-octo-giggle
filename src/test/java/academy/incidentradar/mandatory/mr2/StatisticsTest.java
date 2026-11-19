package academy.incidentradar.mandatory.mr2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import academy.incidentradar.parser.LogParser;
import academy.incidentradar.source.LocalFileSource;
import academy.incidentradar.stats.ResourceCount;
import academy.incidentradar.stats.ResponseSizeStatistics;
import academy.incidentradar.stats.Statistics;
import academy.incidentradar.stats.StatisticsCollector;
import academy.incidentradar.support.TestRecords;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Обязательные тесты: подсчёт статистики. */
@DisplayName("MR2. Статистика")
class StatisticsTest {

    private static final Path SAMPLES = Path.of("samples", "logs");

    private static Statistics collect(Path... files) {
        var parser = new LogParser();
        var collector = new StatisticsCollector();
        for (Path file : files) {
            new LocalFileSource(file).forEachLine(line -> parser.parse(line).ifPresent(collector));
        }
        return collector.build();
    }

    private static ResponseSizeStatistics sizesOf(long... sizes) {
        var collector = new StatisticsCollector();
        for (long size : sizes) {
            collector.accept(TestRecords.request("/", 200, size));
        }
        return collector.build().responseSize();
    }

    private static BigDecimal averageOf(long... sizes) {
        return sizesOf(sizes).average();
    }

    private static long p95Of(long... sizes) {
        return sizesOf(sizes).p95();
    }

    @Test
    @DisplayName("Распределение кодов: 200,200,404,500,500,500 -> 200:2, 404:1, 500:3")
    void responseCodesAreCounted() {
        Statistics stats = collect(SAMPLES.resolve("reference-codes.log"));
        assertThat(stats.responseCodes()).containsExactly(entry(200, 2L), entry(404, 1L), entry(500, 3L));
    }

    @Test
    @DisplayName("Top-10 ресурсов отсортирован по убыванию количества")
    void topResourcesAreSortedDescending() {

        var collector = new StatisticsCollector();
        for (int n = 1; n <= 12; n++) {
            for (int i = 0; i < n; i++) {
                collector.accept(TestRecords.request("/r" + n, 200, 0));
            }
        }

        var top = collector.build().topResources();

        assertThat(top)
                .extracting(ResourceCount::resource)
                .containsExactly("/r12", "/r11", "/r10", "/r9", "/r8", "/r7", "/r6", "/r5", "/r4", "/r3");
        assertThat(top).extracting(ResourceCount::count).containsExactly(12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L);
    }

    @Test
    @DisplayName("Размеры 100..500: average 300, max 500, p95 500")
    void responseSizeStatistics() {
        ResponseSizeStatistics size =
                collect(SAMPLES.resolve("reference-sizes.log")).responseSize();

        assertThat(size.average()).isEqualByComparingTo("300");
        assertThat(size.max()).isEqualTo(500);
        assertThat(size.p95()).isEqualTo(500);
    }

    @Test
    @DisplayName("Дробные значения округляются до 2 знаков после запятой")
    void fractionalValuesAreRoundedToTwoDecimals() {
        assertThat(averageOf(1, 2, 2)).isEqualTo(new BigDecimal("1.67"));
        assertThat(averageOf(0, 0, 0, 0, 0, 0, 0, 1)).isEqualTo(new BigDecimal("0.13"));
        assertThat(averageOf(100, 200)).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("Выбранный метод расчёта p95 зафиксирован и воспроизводим")
    void p95MethodIsDocumentedAndStable() {
        assertThat(p95Of(LongStream.rangeClosed(1, 20).toArray())).isEqualTo(19);
        assertThat(p95Of(LongStream.rangeClosed(1, 20).map(i -> 21 - i).toArray()))
                .isEqualTo(19);
        assertThat(p95Of(LongStream.rangeClosed(1, 10).toArray())).isEqualTo(10);
        assertThat(p95Of(42)).isEqualTo(42);
        Statistics samples =
                collect(SAMPLES.resolve("access-2025-03-01.log"), SAMPLES.resolve("access-2025-03-02.log"));
        assertThat(samples.responseSize().p95()).isEqualTo(5450);
    }
}
