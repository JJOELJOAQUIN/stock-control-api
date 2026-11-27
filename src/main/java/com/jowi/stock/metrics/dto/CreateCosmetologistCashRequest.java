package com.jowi.stock.metrics.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Nueva entrega de efectivo. Sin fecha = hoy (hora Argentina). */
public record CreateCosmetologistCashRequest(
    @NotNull @Positive BigDecimal amount,
    LocalDate date,
    @Size(max = 300) String note) {
}
