package com.jowi.stock.business.dto;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.movement.enums.StockMovementReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Consumo interno de stock: uso personal, traslado a carrito/camilla,
 * muestra, regalo, pedido especial (ej. pedido Luca), etc.
 *
 * Descuenta stock y deja trazabilidad como StockMovement (OUT + motivo +
 * comentario), pero NO genera movimiento de caja ni cuenta como venta, por lo
 * que no infla métricas de ventas.
 */
public record InternalConsumptionRequest(

    @NotNull UUID productId,

    @Positive int quantity,

    @NotNull CashContext context,

    @NotNull StockMovementReason reason,

    @Size(max = 250) String comment
) {}