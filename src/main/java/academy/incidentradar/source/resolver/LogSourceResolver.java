package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LogSource;
import academy.incidentradar.source.LogSourceException;
import java.net.http.HttpClient;
import java.util.List;

public final class LogSourceResolver {

    private final List<SourceResolver> resolvers;

    public LogSourceResolver(List<SourceResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public static LogSourceResolver standard(HttpClient httpClient) {
        return new LogSourceResolver(
                List.of(new UrlSourceResolver(httpClient), new GlobSourceResolver(), new FileSourceResolver()));
    }

    public List<LogSource> resolve(String path) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(path))
                .findFirst()
                .orElseThrow(() -> new LogSourceException("Не удалось определить тип источника: " + path))
                .resolve(path);
    }
}
