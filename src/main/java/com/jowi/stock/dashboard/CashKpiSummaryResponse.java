package com.jowi.stock.dashboard;

import java.math.BigDecimal;

public record CashKpiSummaryResponse(
    BigDecimal totalIn,
    BigDecimal totalOut,
    BigDecimal totalRetention,
    BigDecimal netTotal,

    BigDecimal localNet,
    BigDecimal consultorioNet,

    BigDecimal cashNet,
    BigDecimal transferNet,
    BigDecimal creditNet,
    BigDecimal debitNet
) {}
