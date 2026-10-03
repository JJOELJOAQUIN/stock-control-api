package com.jowi.stock.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Conteo físico de un producto, en su unidad de consumo (ml, ampollas,
 * disparos o unidades). El stock queda EXACTAMENTE en este valor.
 */
public record ReconcileStockRequest(
    @NotNull @Min(0) Integer countedQuantity,
    String comment) {
}
