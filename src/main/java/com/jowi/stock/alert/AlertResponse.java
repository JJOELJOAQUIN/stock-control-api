package com.jowi.stock.alert;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
    AlertType type,
    UUID productId,
    String productName,
    String message,
    OffsetDateTime createdAt
) {}
