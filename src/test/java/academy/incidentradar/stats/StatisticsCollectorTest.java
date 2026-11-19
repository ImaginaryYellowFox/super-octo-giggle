package academy.incidentradar.stats;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.TestRecords;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Сборщик статистики: граничные случаи. */
@DisplayName("StatisticsCollector")
class StatisticsCollectorTest {

    @Test
    @DisplayName("Без записей: получаем нули и пустые списки, без исключений")
    void emptyStatistics() {
        Statistics stats = new StatisticsCollector().build();

        assertThat(stats.totalRequests()).isZero();
        assertThat(stats.responseCodes()).isEmpty();
        assertThat(stats.topResources()).isEmpty();
        assertThat(stats.responseSize()).isEqualTo(new ResponseSizeStatistics(new BigDecimal("0.00"), 0, 0));
    }

    @Test
    @DisplayName("При равном количестве ресурсы упорядочены по алфавиту")
    void tiesAreOrderedAlphabetically() {
        var collector = new StatisticsCollector();
        Stream.of("/b", "/c", "/a", "/c", "/b", "/a", "/c")
                .forEach(resource -> collector.accept(TestRecords.request(resource, 200, 0)));

        assertThat(collector.build().topResources())
                .containsExactly(new ResourceCount("/c", 3), new ResourceCount("/a", 2), new ResourceCount("/b", 2));
    }

    @Test
    @DisplayName("Если меньше 10 ресурсов, то в топ попадают все")
    void fewerThanTenResources() {
        var collector = new StatisticsCollector();
        Stream.of("/a", "/b", "/c").forEach(resource -> collector.accept(TestRecords.request(resource, 200, 0)));

        assertThat(collector.build().topResources()).hasSize(3);
    }
}
