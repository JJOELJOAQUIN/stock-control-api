package com.jowi.stock.cash;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public record CashMovementResponse(
    UUID id,
    CashMovementType type,
    CashSource source,
    PaymentMethod paymentMethod,
    CashContext context,
    BigDecimal amount,
    BigDecimal retention,
    BigDecimal netAmount,
    String comment,
    UUID referenceId,
    Instant createdAt
) {
  public static CashMovementResponse from(CashMovement m) {
    return new CashMovementResponse(
        m.getId(),
        m.getType(),
        m.getSource(),
        m.getPaymentMethod(),
        m.getContext(),
        m.getAmount(),
        m.getRetention(),
        m.getNetAmount(),
        m.getComment(),
        m.getReferenceId(),
        m.getCreatedAt()
    );
  }
}
