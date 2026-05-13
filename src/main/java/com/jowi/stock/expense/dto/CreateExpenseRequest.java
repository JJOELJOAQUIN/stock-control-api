package com.jowi.stock.expense.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

import com.jowi.stock.expense.enums.ExpenseContext;
import com.jowi.stock.expense.enums.ExpenseType;

public record CreateExpenseRequest(
    @NotNull ExpenseType type,
    @NotNull ExpenseContext context,
    @NotNull @Positive BigDecimal amount,
    String comment,
    Boolean recurring
) {}
