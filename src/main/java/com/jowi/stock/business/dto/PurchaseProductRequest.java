package com.jowi.stock.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashContext;

public record PurchaseProductRequest(

                @NotNull UUID productId,

                @Positive int quantity,

                @NotNull BigDecimal amount,

                CashContext context,

                String comment,
                LocalDate expirationDate,
                String lotNumber,
                Boolean updateCostPrice,

                Boolean updateSalePrice,

                BigDecimal newSalePrice,
                Boolean updateMarkupPercentage,
                BigDecimal newDefaultMarkupPercentage) {
}
