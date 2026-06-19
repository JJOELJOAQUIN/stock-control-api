package com.jowi.stock.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalle de un ítem dentro de una orden de compra multi-ítem.
 * Cada ítem corresponde a un producto, con su costo unitario, lote y
 * vencimiento propios. El subtotal del ítem es unitCost * quantity.
 */
public record PurchaseItemRequest(

    @NotNull UUID productId,

    @Positive int quantity,

    @NotNull @PositiveOrZero BigDecimal unitCost,

    LocalDate expirationDate,

    String lotNumber,

    Boolean updateCostPrice,

    Boolean updateSalePrice,

    BigDecimal newSalePrice,

    Boolean updateMarkupPercentage,

    BigDecimal newDefaultMarkupPercentage) {

  /** Subtotal de la línea: costo unitario * cantidad. */
  public BigDecimal subtotal() {
    return unitCost.multiply(BigDecimal.valueOf(quantity));
  }
}