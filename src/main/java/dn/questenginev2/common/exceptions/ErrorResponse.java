package dn.questenginev2.common.exceptions;

import java.time.LocalDateTime;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error
) {}
