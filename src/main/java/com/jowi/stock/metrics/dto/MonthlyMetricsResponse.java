package com.jowi.stock.metrics.dto;

import com.jowi.stock.cash.enums.CashContext;
import java.math.BigDecimal;
import java.util.List;

public record MonthlyMetricsResponse(
    int year,
    int month,
    CashContext context,
    List<ProcedureMetricRow> procedures,
    ProductMetricRow products) {

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
}