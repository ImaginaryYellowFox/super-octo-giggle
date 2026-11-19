package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LocalFileSource;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.LogSourceException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

public final class GlobSourceResolver implements SourceResolver {
    private static final String GLOB_CHARS = "*?[{";

    @Override
    public boolean supports(String path) {
        return firstGlobCharIndex(path) >= 0;
    }

    @Override
    public List<LogSource> resolve(String path) {
        int globStart = firstGlobCharIndex(path);
        int lastSeparator = Math.max(path.lastIndexOf('/', globStart), path.lastIndexOf('\\', globStart));
        Path baseDir = lastSeparator >= 0 ? SourceChecks.toPath(path.substring(0, lastSeparator + 1)) : Path.of(".");
        String globPart = path.substring(lastSeparator + 1).replace('\\', '/');

        if (!Files.isDirectory(baseDir)) {
            throw new LogSourceException("Папка не найдена: " + baseDir);
        }

        PathMatcher matcher;
        try {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPart);
        } catch (IllegalArgumentException e) {
            throw new LogSourceException("Некорректная glob-маска: " + path, e);
        }

        List<LogSource> sources;
        try (Stream<Path> files = Files.walk(baseDir)) {
            sources = files.filter(Files::isRegularFile)
                    .filter(file -> matcher.matches(baseDir.relativize(file)))
                    .filter(file -> SourceChecks.hasSupportedExtension(file.toString()))
                    .sorted()
                    .<LogSource>map(LocalFileSource::new)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось обойти папку " + baseDir, e);
        }

        if (sources.isEmpty()) {
            throw new LogSourceException(
                    "По маске " + path + " не найдено ни одного файла " + SourceChecks.SUPPORTED_EXTENSIONS);
        }
        return sources;
    }

    private static int firstGlobCharIndex(String path) {
        for (int i = 0; i < path.length(); i++) {
            if (GLOB_CHARS.indexOf(path.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
}
