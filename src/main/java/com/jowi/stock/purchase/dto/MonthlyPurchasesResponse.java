package com.jowi.stock.purchase.dto;

import com.jowi.stock.cash.enums.CashContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Compras del mes. Una fila por orden (movimiento de caja PROVIDER_PAYMENT),
 * con sus ítems adentro, y el total gastado del mes en el pie.
 */
public record MonthlyPurchasesResponse(
    int year,
    int month,
    CashContext context,
    BigDecimal totalSpent,
    List<PurchaseOrderRow> orders) {

  public record PurchaseOrderRow(
      String cashMovementId,
      Instant date,
      String paymentMethod,
      String comment,
      BigDecimal total,
      List<PurchaseItemRow> items) {
  }

  public record PurchaseItemRow(
      String productName,
      int quantity,
      BigDecimal unitCost,
      BigDecimal subtotal,
      String lotNumber,
      LocalDate expirationDate) {
  }
}
