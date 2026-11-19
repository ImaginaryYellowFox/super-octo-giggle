package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LocalFileSource;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.LogSourceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileSourceResolver implements SourceResolver {
    @Override
    public boolean supports(String path) {
        return true;
    }

    @Override
    public List<LogSource> resolve(String path) {
        Path file = SourceChecks.toPath(path);
        if (!Files.exists(file)) {
            throw new LogSourceException("Файл не найден: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new LogSourceException("Путь указывает не на файл: " + file);
        }
        if (!SourceChecks.hasSupportedExtension(file.toString())) {
            throw new LogSourceException("Неподдерживаемое расширение файла: " + file + " (поддерживаются "
                    + SourceChecks.SUPPORTED_EXTENSIONS + ")");
        }
        return List.of(new LocalFileSource(file));
    }
}
