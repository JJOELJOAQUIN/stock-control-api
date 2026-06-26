package com.jowi.stock.treatment.dto;

import java.math.BigDecimal;

import com.jowi.stock.cash.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record RegisterPaymentRequest(
    @NotNull BigDecimal amount,
    @NotNull PaymentMethod paymentMethod,
    String comment) {
}