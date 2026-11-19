package academy.incidentradar.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SizeHistogram")
class SizeHistogramTest {

    @Test
    @DisplayName("p95 для 1..100 даёт 95")
    void p95OfOneToHundred() {
        var histogram = new SizeHistogram();
        for (long size = 1; size <= 100; size++) {
            histogram.add(size);
        }

        assertThat(histogram.percentile(95)).isEqualTo(95);
    }

    @Test
    @DisplayName("Повторяющиеся значения: 96 нулей и 4 больших ответа, p95 даёт 0")
    void p95WithRepeatedValues() {
        var histogram = new SizeHistogram();
        for (int i = 0; i < 96; i++) {
            histogram.add(0);
        }
        for (int i = 0; i < 4; i++) {
            histogram.add(1000);
        }

        assertThat(histogram.percentile(95)).isZero();
        assertThat(histogram.max()).isEqualTo(1000);
    }

    @Test
    @DisplayName("p100 совпадает с максимумом")
    void p100IsMax() {
        var histogram = new SizeHistogram();
        histogram.add(10);
        histogram.add(30);
        histogram.add(20);

        assertThat(histogram.percentile(100)).isEqualTo(histogram.max()).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101})
    @DisplayName("Перцентиль вне 1..100 кидает IllegalArgumentException")
    void invalidPercentileIsRejected(int percent) {
        var histogram = new SizeHistogram();
        histogram.add(1);

        assertThatThrownBy(() -> histogram.percentile(percent)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Переполнение суммы кидает ArithmeticException")
    void sumOverflowIsDetected() {
        var histogram = new SizeHistogram();
        histogram.add(Long.MAX_VALUE);

        assertThatThrownBy(() -> histogram.add(1)).isInstanceOf(ArithmeticException.class);
    }
}
