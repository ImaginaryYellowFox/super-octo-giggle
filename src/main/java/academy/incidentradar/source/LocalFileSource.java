package academy.incidentradar.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

public record LocalFileSource(Path path) implements LogSource {

    @Override
    public String name() {
        return path.toString();
    }

    @Override
    public void forEachLine(Consumer<String> consumer) {
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.forEach(consumer);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл " + path, e);
        }
    }
}
