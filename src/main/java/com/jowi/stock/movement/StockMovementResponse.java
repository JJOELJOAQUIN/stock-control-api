package com.jowi.stock.movement;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
    UUID id,
    UUID productId,
    StockMovementType type,
    StockMovementReason reason,
    int quantity,
    String comment,
    Instant createdAt
) {

  public static StockMovementResponse from(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getProduct().getId(),
        movement.getType(),
        movement.getReasonType(),
        movement.getQuantity(),
        movement.getComment(),
        movement.getCreatedAt()
    );
  }
}
