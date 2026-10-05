package com.jowi.stock.stock.dto;

import java.util.UUID;

/** Resultado de la reconciliación: cuánto había, cuánto quedó y la diferencia. */
public record ReconcileStockResponse(
    UUID productId,
    int previous,
    int current,
    int difference) {
}
