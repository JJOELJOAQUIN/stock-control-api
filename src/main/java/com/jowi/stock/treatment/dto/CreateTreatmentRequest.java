package com.jowi.stock.treatment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

/**
 * Crea un tratamiento y, opcionalmente, registra su primer pago.
 * cosmetologistFixedShare es opcional (override del default global).
 */
public record CreateTreatmentRequest(
    @NotNull String procedureCode,
    String procedureLabel,
    UUID patientId,
    @NotNull CashContext context,
    @NotNull BigDecimal totalAmount,
    BigDecimal cosmetologistFixedShare,
    String comment,
    // Primer pago (opcional): si viene, se registra junto con el tratamiento.
    BigDecimal firstPaymentAmount,
    PaymentMethod firstPaymentMethod) {
}