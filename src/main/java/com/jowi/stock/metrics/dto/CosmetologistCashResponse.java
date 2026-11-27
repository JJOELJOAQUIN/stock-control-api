package com.jowi.stock.metrics.dto;

import com.jowi.stock.metrics.entities.CosmetologistCashDeposit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Efectivo que dejó la cosmetóloga en un mes, con el total. */
public record CosmetologistCashResponse(
    int year,
    int month,
    BigDecimal total,
    List<Entry> entries) {

  public record Entry(
      UUID id,
      LocalDate date,
      BigDecimal amount,
      String note,
      String createdBy,
      Instant createdAt) {

    public static Entry from(CosmetologistCashDeposit d) {
      return new Entry(
          d.getId(), d.getDepositDate(), d.getAmount(), d.getNote(),
          d.getCreatedBy(), d.getCreatedAt());
    }
  }
}
