package com.jowi.stock.dashboard;

import java.math.BigDecimal;

public record CashKpiByPaymentMethodResponse(
    String paymentMethod,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal retainedAmount,
    BigDecimal netAmount
) {}
