package com.jowi.stock.cash.dto;

import java.math.BigDecimal;

import com.jowi.stock.cash.enums.CashContext;

/**
 * Totales históricos de ingresos por contexto, separados por origen.
 *
 * @param context          contexto consultado
 * @param productSales     total histórico de ventas de productos (PRODUCT_SALE)
 * @param procedureIncome  total histórico de ingresos por procedimientos (PROCEDURE)
 */
public record CashSalesTotalsResponse(
    CashContext context,
    BigDecimal productSales,
    BigDecimal procedureIncome
) {}
