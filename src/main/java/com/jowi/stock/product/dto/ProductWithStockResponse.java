package com.jowi.stock.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductWithStockResponse(
        UUID id,
        String name,
        String barcode,
        String brand,
        String category,
        String scope,
        Integer minimumStock,
        Integer currentStock,
        Boolean belowMinimum,
        Boolean active,
        BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal defaultMarkupPercentage,
        Integer shelfLifeMonths,
        Integer restockPriority,
        String consumptionUnit,
        // Unidades de consumo por envase (ej. NCTF: 10 dosis por caja). La
        // reconciliación lo usa para cargar "envases cerrados + sueltos".
        Integer unitsPerPackage) {
}
