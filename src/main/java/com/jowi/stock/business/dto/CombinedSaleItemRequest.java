package com.jowi.stock.business.dto;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ítem de una venta combinada (intent-based; el backend calcula los montos).
 *
 * PRODUCT   -> productId + performedBy (MEDICA/COSMETOLOGA) obligatorios.
 * PROCEDURE -> procedureCode + doctor/cosmetologistSharePercent (suman 1).
 */
public record CombinedSaleItemRequest(
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
