package com.jowi.stock.metrics.dto;

import com.jowi.stock.cash.enums.CashContext;
import java.math.BigDecimal;
import java.util.List;

public record MonthlyMetricsResponse(
    int year,
    int month,
    CashContext context,
    List<ProcedureMetricRow> procedures,
    ProductMetricRow products,
    List<ProductDetailRow> productDetail) {

  public record ProcedureMetricRow(
      String procedureCode,
      long count,
      BigDecimal amount,
      BigDecimal netAmount,
      BigDecimal doctorShare,
      BigDecimal cosmetologistShare) {
  }

  public record ProductMetricRow(
      long count,
      BigDecimal amount,
      BigDecimal netAmount,
      BigDecimal doctorShare,
      BigDecimal cosmetologistShare) {
  }

  /**
   * Detalle por producto vendido en el mes. La ganancia de la médica es
   * lo cobrado menos el costo de la mercadería menos la comisión de la
   * cosmetóloga:  profit = revenue - cost - commission.
   *
   * - revenue:    lo efectivamente cobrado (subtotal, ya con descuento).
   * - cost:       cantidad * costo unitario del producto (costPrice).
   * - commission: 5% que se lleva Gise cuando la venta la hizo ella.
   * - profit:     ganancia real para Pili.
   */
  public record ProductDetailRow(
      String productId,
      String name,
      long count,
      BigDecimal revenue,
      BigDecimal cost,
      BigDecimal commission,
      BigDecimal profit) {
  }
}