package academy.incidentradar.source;

public final class LogSourceException extends RuntimeException {

    public LogSourceException(String message) {
        super(message);
    }

    public LogSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
