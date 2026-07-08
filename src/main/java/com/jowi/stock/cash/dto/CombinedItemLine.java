package com.jowi.stock.cash.dto;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Línea normalizada para persistir una venta combinada. La calcula el
 * BusinessOperationService y la consume CashMovementService.createCombined.
 *
 * PRODUCT   -> performedBy define el reparto (regla producto: MEDICA 100% neto,
 *              COSMETOLOGA 5% neto). procedureCode/percents en null.
 * PROCEDURE -> doctor/cosmetologistSharePercent (suman 1). performedBy en null.
 */
public record CombinedItemLine(
    CashMovementItemKind kind,
    UUID productId,
    String procedureCode,
    String description,
    int quantity,
    BigDecimal unitAmount,
    BigDecimal subtotal,
    CashActor performedBy,
    BigDecimal doctorSharePercent,
    BigDecimal cosmetologistSharePercent) {
}