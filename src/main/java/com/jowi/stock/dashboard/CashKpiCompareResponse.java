package com.jowi.stock.dashboard;

import java.math.BigDecimal;

public record CashKpiCompareResponse(

    // valores mes actual
    BigDecimal income,
    BigDecimal expense,
    BigDecimal retention,
    BigDecimal net,

    // diferencias vs mes anterior
    BigDecimal incomeDiff,
    BigDecimal expenseDiff,
    BigDecimal retentionDiff,
    BigDecimal netDiff,

    // variaciones porcentuales
    BigDecimal incomePct,
    BigDecimal expensePct,
    BigDecimal retentionPct,
    BigDecimal netPct
) {}
