package com.jowi.stock.cash;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProcedureCashRequest(
    CashMovementType type,
    BigDecimal amount,
    BigDecimal doctorPercent,
    BigDecimal cosmetologistPercent,
    String comment,
    UUID referenceId
) {}

