package com.jowi.stock.business.dto;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import java.math.BigDecimal;
import java.util.List;
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
    BigDecimal cosmetologistSharePercent,
    /**
     * Opcional, solo PROCEDURE: insumos REALMENTE usados en esta sesión, en
     * unidades guardadas (ML en décimas). Reemplaza a la receta. Solo se
     * acepta de un ADMIN (la médica: cicatrices según tamaño, DMAE
     * reemplazable); para el resto manda la receta. Null = receta.
     */
    List<ConsumptionLine> consumptions) {

  public record ConsumptionLine(UUID productId, int quantity) {
  }
}
