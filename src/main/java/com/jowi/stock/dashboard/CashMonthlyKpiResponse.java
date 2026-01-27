package com.jowi.stock.dashboard;

import java.math.BigDecimal;

public record CashMonthlyKpiResponse(
    int month,                 // 1..12
    BigDecimal income,          // ingresos brutos
    BigDecimal expense,         // egresos
    BigDecimal retention,       // retenciones
    BigDecimal net              // income - expense - retention
) {}
