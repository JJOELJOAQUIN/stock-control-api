package com.jowi.stock.cash.dto;

import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import com.jowi.stock.cash.enums.SplitPreset;

/**
 * Datos del ítem espejo que acompaña a un movimiento de caja creado con shares
 * fijos. Null en createWithFixedShares = no generar ítem.
 *
 * Va agrupado en un record y no como cuatro parámetros sueltos porque
 * createWithFixedShares ya tiene diez, y sumarle cuatro más lo dejaba en el
 * mismo estado que CreateCashMovementRequest: una fila de nulls posicionales
 * donde un error de posición compila y registra plata mal.
 *
 * @param kind         PRODUCT o PROCEDURE
 * @param productId    sólo para PRODUCT
 * @param procedureCode sólo para PROCEDURE
 * @param description  descripción del ítem (se recorta a 200)
 * @param performedBy  quién hizo el trabajo — el dato del que depende la card
 *                     de la cosmetóloga
 * @param splitPreset  reparto aplicado; sólo peeling profundo lo usa
 */
public record CashItemSpec(
    CashMovementItemKind kind,
    UUID productId,
    String procedureCode,
    String description,
    CashActor performedBy,
    SplitPreset splitPreset) {
}