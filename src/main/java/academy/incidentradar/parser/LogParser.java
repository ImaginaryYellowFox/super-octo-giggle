package academy.incidentradar.parser;

import academy.incidentradar.model.LogRecord;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogParser {

    private static final Pattern LINE = Pattern.compile("^(?<remoteAddr>\\S+) - (?<remoteUser>\\S+) "
            + "\\[(?<time>[^\\]]+)] "
            + "\"(?<method>\\S+) (?<resource>\\S+) (?<protocol>\\S+)\" "
            + "(?<status>\\d{3}) (?<bytes>\\d+) "
            + "\"(?<referer>[^\"]*)\" \"(?<userAgent>[^\"]*)\"$");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    public Optional<LogRecord> parse(String line) {
        Matcher matcher = LINE.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new LogRecord(
                    group(matcher, "remoteAddr"),
                    group(matcher, "remoteUser"),
                    OffsetDateTime.parse(group(matcher, "time"), TIME_FORMAT),
                    group(matcher, "method"),
                    group(matcher, "resource"),
                    group(matcher, "protocol"),
                    Integer.parseInt(group(matcher, "status")),
                    Long.parseLong(group(matcher, "bytes")),
                    group(matcher, "referer"),
                    group(matcher, "userAgent")));
        } catch (DateTimeParseException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String group(Matcher matcher, String name) {
        String value = matcher.group(name);
        if (value == null) {
            throw new IllegalStateException("Группа " + name + " не найдена в регулярном выражении");
        }
        return value;
    }
}
