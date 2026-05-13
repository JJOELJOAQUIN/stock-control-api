package com.jowi.stock.movement.dto;

import java.time.Instant;
import java.util.UUID;

import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;

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
