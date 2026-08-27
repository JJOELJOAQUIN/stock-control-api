package com.jowi.stock.cash.dto;

import com.jowi.stock.cash.enums.CashContext;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cierre de caja de un día. date opcional: null = hoy. note opcional.
 */
public record CloseCashRequest(
    @NotNull CashContext context,
    LocalDate date,
    String note) {
}
