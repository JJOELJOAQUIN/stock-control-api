package com.jowi.stock.business.dto;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;

/**
 * Venta combinada: uno o varios productos y/o procedimientos en una única
 * operación. Genera un solo CashMovement (cabecera) con N ítems de detalle.
 *
 * @param expectedTotal total calculado por el cliente, validado contra la suma
 *                      de subtotales en el backend (tolerancia de un centavo).
 * @param performedBy   quién realizó la venta (MEDICA / COSMETOLOGA). Obligatorio.
 */
public record CombinedSaleRequest(
    CashContext context,
    PaymentMethod paymentMethod,
    String comment,
    CashActor performedBy,
    BigDecimal expectedTotal,
    List<CombinedSaleItemRequest> items) {
}
