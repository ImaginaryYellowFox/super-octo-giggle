package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LogSourceException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

final class SourceChecks {
    static final List<String> SUPPORTED_EXTENSIONS = List.of(".log", ".txt");

    private SourceChecks() {}

    static boolean hasSupportedExtension(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    static Path toPath(String value) {
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new LogSourceException("Некорректный путь: " + value, e);
        }
    }
}
