package academy.incidentradar.filter;

import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public record DateRange(@Nullable LocalDate from, @Nullable LocalDate to) implements Predicate<LogRecord> {

    public static DateRange unbounded() {
        return new DateRange(null, null);
    }

    @Override
    public boolean test(LogRecord record) {
        LocalDate date = record.time().toLocalDate();
        boolean afterFrom = from == null || !date.isBefore(from);
        boolean beforeTo = to == null || !date.isAfter(to);
        return afterFrom && beforeTo;
    }
}
