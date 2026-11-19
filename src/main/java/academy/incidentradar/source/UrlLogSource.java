package academy.incidentradar.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;

public record UrlLogSource(URI uri, HttpClient httpClient) implements LogSource {

    private static final int HTTP_OK = 200;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    public static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String name() {
        return uri.toString();
    }

    @Override
    public void forEachLine(Consumer<String> consumer) {
        HttpRequest request =
                HttpRequest.newBuilder(uri).timeout(RESPONSE_TIMEOUT).GET().build();

        try {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            try (Stream<String> lines = response.body()) {
                if (response.statusCode() != HTTP_OK) {
                    throw new LogSourceException("URL " + uri + " вернул HTTP " + response.statusCode());
                }
                lines.forEach(consumer);
            }
        } catch (ConnectException e) {
            throw new LogSourceException("Не удалось подключиться к " + uri, e);
        } catch (HttpTimeoutException e) {
            throw new LogSourceException("Сервер не ответил вовремя: " + uri, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Ошибка при чтении " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogSourceException("Чтение " + uri + " прервано", e);
        }
    }
}
