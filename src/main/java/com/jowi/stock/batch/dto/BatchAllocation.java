package com.jowi.stock.batch.dto;

import java.util.UUID;

/**
 * Cuánto se tomó de un lote puntual en una salida de stock.
 *
 * Es el dato que permite que la anulación de una venta devuelva las
 * unidades al MISMO lote del que salieron, en vez de adivinar. Sin esto,
 * revertir una venta de 3 unidades ensucia las alertas de vencimiento:
 * no hay forma de saber si salieron del lote que vence en marzo o del
 * que vence en agosto.
 */
public record BatchAllocation(UUID batchId, int quantity) {
}