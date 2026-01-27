package com.jowi.stock.business;

import com.jowi.stock.cash.CashContext;
import com.jowi.stock.cash.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record SellProductRequest(

    @NotNull UUID productId,

    @Positive int quantity,

    @NotNull BigDecimal amount,

    @NotNull PaymentMethod paymentMethod,

    @NotNull CashContext context,

    String comment
) {}
