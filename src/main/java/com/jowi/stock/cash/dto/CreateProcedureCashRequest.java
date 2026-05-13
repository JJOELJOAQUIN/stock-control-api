package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashMovementType;

public record CreateProcedureCashRequest(
    CashMovementType type,
    BigDecimal amount,
    BigDecimal doctorPercent,
    BigDecimal cosmetologistPercent,
    String comment,
    UUID referenceId
) {}

