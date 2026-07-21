package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;

/**
 * Request genérico de movimiento de caja.
 *
 * Nota histórica: una versión intermedia le agregó procedureCode,
 * splitPreset y peelingPaymentKind para el reparto del peeling. Ese camino
 * se abandonó — el peeling se cobra por Tratamientos y su preset viaja en
 * RegisterPaymentRequest — así que este record volvió a sus 12 componentes.
 * Si alguna vez ves llamadas con 15 argumentos, son restos de esa versión.
 */
public record CreateCashMovementRequest(
    CashMovementType type,
    CashSource source,
    PaymentMethod paymentMethod,
    CashContext context,
    BigDecimal amount,
    BigDecimal retentionPercent,
    String comment,
    String detail,
    UUID referenceId,
    BigDecimal doctorSharePercent,
    BigDecimal cosmetologistSharePercent,
    CashActor performedBy) {
}