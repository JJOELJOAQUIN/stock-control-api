package com.jowi.stock.product.dto;

import java.math.BigDecimal;

public record ProductScanWithStockResponse(
        String id,
        String name,
        String barcode,
        String scope,
        int currentStock,
        boolean belowMinimum,
        BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal defaultMarkupPercentage) {
}
