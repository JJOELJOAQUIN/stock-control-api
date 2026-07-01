package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.jowi.stock.cash.enums.CashContext;

/**
 * Desglose de la producción de la COSMETÓLOGA para una fecha:
 * sus procedimientos y sus ventas, mostrando cuánto se lleva ella y
 * cuánto va para la médica en cada caso.
 *
 * Se calcula sobre los movimientos con cosmetologistShare > 0, que son
 * exactamente los que produjo la cosmetóloga (procedimientos de cosmetología
 * y ventas hechas por ella), excluyendo lo propio de la médica.
 */
public record CashCosmetologistSplitResponse(
    LocalDate date,
    CashContext context,
    BigDecimal procedureCosmetologist,
    BigDecimal procedureDoctor,
    BigDecimal salesCosmetologist,
    BigDecimal salesDoctor
) {}