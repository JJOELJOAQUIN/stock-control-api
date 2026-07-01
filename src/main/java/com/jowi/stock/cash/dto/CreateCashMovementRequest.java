package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;

public record CreateCashMovementRequest(
                CashMovementType type,
                CashSource source,
                PaymentMethod paymentMethod,
                CashContext context,
                BigDecimal amount,
                BigDecimal retentionPercent,
                String comment,
                String detail,
                UUID referenceId,
                BigDecimal doctorSharePercent,
                BigDecimal cosmetologistSharePercent,
                CashActor performedBy) {
}