package com.jowi.stock.cash;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashDailySplitResponse(
    LocalDate date,
    CashContext context,
    BigDecimal netIncome,
    BigDecimal doctorTotal,
    BigDecimal cosmetologistTotal
) {}