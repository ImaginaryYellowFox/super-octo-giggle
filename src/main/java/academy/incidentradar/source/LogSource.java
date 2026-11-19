package academy.incidentradar.source;

import java.util.function.Consumer;

public interface LogSource {

    String name();

    void forEachLine(Consumer<String> consumer);
}
