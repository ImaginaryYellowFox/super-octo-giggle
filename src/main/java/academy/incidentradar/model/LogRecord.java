package academy.incidentradar.model;

import java.time.OffsetDateTime;

public record LogRecord(
        String remoteAddr,
        String remoteUser,
        OffsetDateTime time,
        String method,
        String resource,
        String protocol,
        int status,
        long bodyBytesSent,
        String referer,
        String userAgent) {}
