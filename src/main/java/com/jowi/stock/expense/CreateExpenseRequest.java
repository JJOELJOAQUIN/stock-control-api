package com.jowi.stock.expense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateExpenseRequest(
    @NotNull ExpenseType type,
    @NotNull ExpenseContext context,
    @NotNull @Positive BigDecimal amount,
    String comment,
    Boolean recurring
) {}
