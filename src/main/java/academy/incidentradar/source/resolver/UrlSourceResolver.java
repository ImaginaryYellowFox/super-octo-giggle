package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.LogSourceException;
import academy.incidentradar.source.UrlLogSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;

public final class UrlSourceResolver implements SourceResolver {
    private final HttpClient httpClient;

    public UrlSourceResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public boolean supports(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    @Override
    public List<LogSource> resolve(String path) {
        URI uri;
        try {
            uri = new URI(path);

        } catch (URISyntaxException e) {
            throw new LogSourceException("Некорректный URL: " + path, e);
        }

        String urlPath = uri.getPath();
        if (uri.getHost() == null || urlPath == null) {
            throw new LogSourceException("Некорректный URL: " + path);
        }
        if (!SourceChecks.hasSupportedExtension(urlPath)) {
            throw new LogSourceException("Неподдерживаемое расширение в URL: " + path + " (поддерживаются "
                    + SourceChecks.SUPPORTED_EXTENSIONS + ")");
        }
        return List.of(new UrlLogSource(uri, httpClient));
    }
}
