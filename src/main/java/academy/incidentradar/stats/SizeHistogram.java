package academy.incidentradar.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class SizeHistogram {

    private static final int DECIMAL_PLACES = 2;
    private static final long PERCENT_SCALE = 100;

    private final NavigableMap<Long, Long> counts = new TreeMap<>();
    private long count;
    private long sum;

    void add(long size) {
        counts.merge(size, 1L, Long::sum);
        count++;
        sum = Math.addExact(sum, size);
    }

    BigDecimal average() {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(DECIMAL_PLACES);
        }
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    long max() {
        return count == 0 ? 0 : counts.lastKey();
    }

    long percentile(int percent) {
        if (percent <= 0 || percent > PERCENT_SCALE) {
            throw new IllegalArgumentException("Перцентиль должен быть в диапазоне 1..100: " + percent);
        }
        if (count == 0) {
            return 0;
        }

        long rank = Math.ceilDiv(percent * count, PERCENT_SCALE);
        long seen = 0;
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            seen += entry.getValue();
            if (seen >= rank) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Ранг " + rank + " больше числа значений " + count);
    }
}
