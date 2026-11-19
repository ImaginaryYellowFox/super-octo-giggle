package academy.incidentradar.source.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.source.LocalFileSource;
import academy.incidentradar.source.LogSourceException;
import academy.incidentradar.source.UrlLogSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("LogSourceResolver")
class LogSourceResolverTest {

    private HttpClient httpClient;
    private LogSourceResolver resolver;

    @BeforeEach
    void setUp() {
        httpClient = UrlLogSource.newHttpClient();
        resolver = LogSourceResolver.standard(httpClient);
    }

    @AfterEach
    void tearDown() {
        httpClient.close();
    }

    @Test
    @DisplayName("Файл .txt принимается наравне с .log")
    void acceptsTxtFile(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("access.txt"), "line\n");

        assertThat(resolver.resolve(file.toString())).containsExactly(new LocalFileSource(file));
    }

    @Test
    @DisplayName("Расширение проверяется без учёта регистра")
    void extensionIsCaseInsensitive(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("ACCESS.LOG"), "line\n");

        assertThat(resolver.resolve(file.toString())).containsExactly(new LocalFileSource(file));
    }

    @Test
    @DisplayName("Файл с неподдерживаемым расширением приводит к ошибке")
    void rejectsUnsupportedFileExtension(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("notes.md"), "line\n");

        assertThatThrownBy(() -> resolver.resolve(file.toString()))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Неподдерживаемое расширение");
    }

    @Test
    @DisplayName("Путь к папке вместо файла приводит к ошибке")
    void rejectsDirectory(@TempDir Path dir) throws IOException {
        Path folder = Files.createDirectory(dir.resolve("logs"));

        assertThatThrownBy(() -> resolver.resolve(folder.toString()))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("не на файл");
    }

    @Test
    @DisplayName("Маска ** находит файлы во вложенных папках")
    void globDoubleStarSearchesSubdirectories(@TempDir Path dir) throws IOException {
        Path sub = Files.createDirectories(dir.resolve("sub"));
        Path deeper = Files.createDirectories(sub.resolve("deeper"));
        Files.writeString(dir.resolve("root.log"), "r\n");
        Path a = Files.writeString(sub.resolve("a.log"), "a\n");
        Path b = Files.writeString(deeper.resolve("b.log"), "b\n");

        String pattern = dir + File.separator + "**" + File.separator + "*.log";

        assertThat(resolver.resolve(pattern)).containsExactly(new LocalFileSource(a), new LocalFileSource(b));
    }

    @Test
    @DisplayName("Маска без совпадений приводит  к ошибке")
    void globWithoutMatchesFails(@TempDir Path dir) {
        assertThatThrownBy(() -> resolver.resolve(dir + File.separator + "*.log"))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("не найдено");
    }

    @Test
    @DisplayName("Маска в несуществующей папке приводит к ошибке")
    void globInMissingDirectoryFails(@TempDir Path dir) {
        String pattern = dir + File.separator + "missing" + File.separator + "*.log";

        assertThatThrownBy(() -> resolver.resolve(pattern))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Папка не найдена");
    }

    @Test
    @DisplayName("Синтаксически неверная маска приводит к ошибке")
    void invalidGlobFails(@TempDir Path dir) {
        String pattern = dir + File.separator + "access-[0-9.log";

        assertThatThrownBy(() -> resolver.resolve(pattern))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Некорректная glob-маска");
    }

    @Test
    @DisplayName("URL с ? распознаётся как URL, а не как маска")
    void urlWithQueryIsNotTreatedAsGlob() {
        String url = "http://127.0.0.1:8000/access.log?date=today";

        assertThat(resolver.resolve(url)).containsExactly(new UrlLogSource(URI.create(url), httpClient));
    }

    @Test
    @DisplayName("URL с неподдерживаемым расширением приводит к ошибке ещё до запроса")
    void rejectsUnsupportedUrlExtension() {
        assertThatThrownBy(() -> resolver.resolve("http://127.0.0.1:8000/report.html"))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Неподдерживаемое расширение в URL");
    }

    @Test
    @DisplayName("Синтаксически неверный URL приводит к ошибке")
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> resolver.resolve("http://bad host/access.log"))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Некорректный URL");
    }

    @Test
    @DisplayName("Ни один резолвер не подошёл - кидается ошибка")
    void noMatchingResolverFails() {
        var empty = new LogSourceResolver(List.of());

        assertThatThrownBy(() -> empty.resolve("anything.log"))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Не удалось определить тип источника");
    }
}
