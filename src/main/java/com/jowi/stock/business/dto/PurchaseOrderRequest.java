package com.jowi.stock.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;

/**
 * Orden de compra multi-ítem.
 *
 * Cabecera común (contexto, comentario/proveedor, método de pago) + detalle
 * de ítems (uno por producto, con su lote/vencimiento). Genera un único
 * movimiento de caja por el monto total de la compra.
 *
 * El campo {@code expectedTotal} es el total calculado por el cliente; el
 * backend lo recalcula desde los ítems y valida que coincidan, para garantizar
 * la integridad del movimiento de caja.
 */
public record PurchaseOrderRequest(

    CashContext context,

    String comment,

    @NotNull PaymentMethod paymentMethod,

    @NotNull @PositiveOrZero BigDecimal expectedTotal,

    @NotEmpty @Valid List<PurchaseItemRequest> items) {
}