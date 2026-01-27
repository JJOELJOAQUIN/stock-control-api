package com.jowi.stock.business;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseProductRequest(

    @NotNull UUID productId,

    @Positive int quantity,

    @NotNull BigDecimal amount,

    String comment
) {}
