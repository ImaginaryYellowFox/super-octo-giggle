package academy.incidentradar.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import academy.incidentradar.support.TestHttpServer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UrlLogSource")
class UrlLogSourceTest {

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = UrlLogSource.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        httpClient.close();
    }

    private static String urlOfStoppedServer() {
        try (var server = TestHttpServer.start()) {
            return server.url("/access.log");
        }
    }

    @Test
    @DisplayName("Несколько URL читаются по очереди, строки не теряются")
    void readsSeveralUrls() {
        try (var server = TestHttpServer.start()) {
            server.respond("/day1.log", HttpURLConnection.HTTP_OK, "a1\na2\n")
                    .respond("/day2.log", HttpURLConnection.HTTP_OK, "b1\n");

            List<String> lines = new ArrayList<>();
            for (String path : List.of("/day1.log", "/day2.log")) {
                new UrlLogSource(URI.create(server.url(path)), httpClient).forEachLine(lines::add);
            }

            assertThat(lines).containsExactly("a1", "a2", "b1");
        }
    }

    @Test
    @DisplayName("Ответ 500 -> LogSourceException с кодом ответа")
    void serverErrorFails() {
        try (var server = TestHttpServer.start()) {
            server.respond("/broken.log", HttpURLConnection.HTTP_INTERNAL_ERROR, "oops");
            var source = new UrlLogSource(URI.create(server.url("/broken.log")), httpClient);

            assertThatThrownBy(() -> source.forEachLine(line -> {}))
                    .isInstanceOf(LogSourceException.class)
                    .hasMessageContaining("HTTP 500");
        }
    }

    @Test
    @DisplayName("Сервер не запущен -> LogSourceException")
    void unreachableServerFails() {
        var source = new UrlLogSource(URI.create(urlOfStoppedServer()), httpClient);

        assertThatThrownBy(() -> source.forEachLine(line -> {}))
                .isInstanceOf(LogSourceException.class)
                .hasMessageContaining("Не удалось подключиться");
    }
}
