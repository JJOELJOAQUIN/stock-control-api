package com.jowi.stock.product;


public record ProductScanWithStockResponse(
    String id,
    String name,
    String barcode,
    String scope,
    int currentStock,
    boolean belowMinimum
) {}
