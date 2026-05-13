package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jowi.stock.cash.enums.CashContext;

public record CashDailySplitResponse(
    LocalDate date,
    CashContext context,
    BigDecimal netIncome,
    BigDecimal doctorTotal,
    BigDecimal cosmetologistTotal
) {}