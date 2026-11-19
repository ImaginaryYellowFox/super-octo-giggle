package academy.incidentradar.stats;

import java.math.BigDecimal;

public record ResponseSizeStatistics(BigDecimal average, long max, long p95) {}
