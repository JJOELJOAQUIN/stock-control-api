package com.jowi.stock.cash.dto;

import com.jowi.stock.cash.enums.CashContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Resumen de caja de un día por método de pago. Sirve tanto para el "preview"
 * en vivo (antes de cerrar) como para leer un cierre ya persistido.
 *
 * - closed / closedBy / closedAt: sólo tienen valor si el día ya está cerrado.
 * - Cada monto de método es NETO (entradas menos salidas).
 */
public record DailyCashSummaryResponse(
    CashContext context,
    LocalDate date,
    BigDecimal cashNet,
    BigDecimal transferNet,
    BigDecimal debitNet,
    BigDecimal creditNet,
    BigDecimal totalIn,
    BigDecimal totalOut,
    BigDecimal netTotal,
    boolean closed,
    String closedBy,
    Instant closedAt,
    String note) {
}