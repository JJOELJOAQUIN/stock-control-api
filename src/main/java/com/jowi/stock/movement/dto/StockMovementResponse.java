package com.jowi.stock.movement.dto;

import com.jowi.stock.product.enums.ConsumptionUnit;

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
    Instant createdAt,
    // quantity va en unidades guardadas: dividir por unitScale (ML = 10).
    String unit,
    int unitScale
) {

  public static StockMovementResponse from(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getProduct().getId(),
        movement.getType(),
        movement.getReasonType(),
        movement.getQuantity(),
        movement.getComment(),
        movement.getCreatedAt(),
        movement.getProduct().getConsumptionUnit() == null
            ? "UNIDAD"
            : movement.getProduct().getConsumptionUnit().name(),
        ConsumptionUnit.scaleOf(movement.getProduct().getConsumptionUnit())
    );
  }
}
