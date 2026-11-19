package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.source.LocalFileSource;
import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.UrlLogSource;
import academy.incidentradar.source.resolver.LogSourceResolver;
import academy.incidentradar.support.CliRunner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Обязательные тесты: источники логов. */
@DisplayName("MR1. Источники логов")
class LogSourceTest {

    private static final int USAGE_ERROR = 2;
    private static final Path SAMPLES = Path.of("samples", "logs");

    private static final List<String> REMOTE_LINES = List.of(
            "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"",
            "217.168.17.5 - - [17/May/2015:08:05:34 +0000] \"GET /downloads/product_1 HTTP/1.1\" 200 490 \"-\" \"Debian APT-HTTP/1.3\"");

    private HttpServer server;
    private String baseUrl;
    private HttpClient httpClient;
    private LogSourceResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/access.log",
                exchange -> respond(exchange, HttpURLConnection.HTTP_OK, String.join("\n", REMOTE_LINES) + "\n"));
        server.createContext(
                "/missing.log", exchange -> respond(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Not Found"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        httpClient = UrlLogSource.newHttpClient();
        resolver = LogSourceResolver.standard(httpClient);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        httpClient.close();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static List<String> readAll(List<LogSource> sources) {
        List<String> lines = new ArrayList<>();
        sources.forEach(source -> source.forEachLine(lines::add));
        return lines;
    }

    @Test
    @DisplayName("Локальный файл читается")
    void readsLocalFile() throws IOException {
        Path file = SAMPLES.resolve("reference-codes.log");

        List<LogSource> sources = resolver.resolve(file.toString());

        assertThat(sources).singleElement().isInstanceOf(LocalFileSource.class);
        assertThat(readAll(sources)).containsExactlyElementsOf(Files.readAllLines(file));
    }

    @Test
    @DisplayName("Glob-маска: в отчёт попадают все подошедшие файлы")
    void readsAllFilesMatchingGlob(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("access-1.log"), "a1\na2\n");
        Files.writeString(dir.resolve("access-2.log"), "b1\n");
        Files.writeString(dir.resolve("access-3.txt"), "c1\n");
        Files.writeString(dir.resolve("error-1.log"), "e1\n");

        List<LogSource> sources = resolver.resolve(dir + File.separator + "access-*.log");

        assertThat(sources)
                .extracting(LogSource::name)
                .containsExactly(
                        dir.resolve("access-1.log").toString(),
                        dir.resolve("access-2.log").toString());
        assertThat(readAll(sources)).containsExactly("a1", "a2", "b1");
    }

    @Test
    @DisplayName("Лог читается по URL")
    void readsRemoteFile() {
        List<LogSource> sources = resolver.resolve(baseUrl + "/access.log");

        assertThat(sources).singleElement().isInstanceOf(UrlLogSource.class);
        assertThat(readAll(sources)).containsExactlyElementsOf(REMOTE_LINES);
    }

    @Test
    @DisplayName("URL вернул 404 -> exit code 2")
    void remote404ExitsWithTwo() {
        var result = CliRunner.run("--path", baseUrl + "/missing.log");

        assertThat(result.exitCode())
                .as("Ожидался код 2 (ошибка использования).%n%s", result)
                .isEqualTo(USAGE_ERROR);
        assertThat(result.stderr()).contains("HTTP 404");
    }
}
