package com.jowi.stock.cash;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCashMovementRequest(
        CashMovementType type,
        CashSource source,
        PaymentMethod paymentMethod,
        CashContext context,
        BigDecimal amount,
        BigDecimal retentionPercent,
        String comment,
        UUID referenceId) {
}
