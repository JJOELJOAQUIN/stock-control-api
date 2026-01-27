package com.jowi.stock.dashboard;

import java.math.BigDecimal;

public record CashKpiByContextResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal retainedAmount,
    BigDecimal netBalance
) {}
