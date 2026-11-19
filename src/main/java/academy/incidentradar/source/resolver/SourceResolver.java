package academy.incidentradar.source.resolver;

import academy.incidentradar.source.LogSource;
import java.util.List;

public interface SourceResolver {
    boolean supports(String path);

    List<LogSource> resolve(String path);
}
