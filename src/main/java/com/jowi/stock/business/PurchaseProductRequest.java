package com.jowi.stock.business;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.CashContext;

public record PurchaseProductRequest(

    @NotNull UUID productId,

    @Positive int quantity,

    @NotNull BigDecimal amount,

    CashContext context,

    String comment
) {}
