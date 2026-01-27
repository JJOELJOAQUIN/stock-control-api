package com.jowi.stock.stock;

import java.util.UUID;

public record LowStockResponse(
    UUID productId,
    int current,
    int minimum,
    boolean belowMinimum
) {
  public static LowStockResponse from(Stock stock) {
    return new LowStockResponse(
        stock.getProductId(),
        stock.getCurrent(),
        stock.getMinimum(),
        stock.isBelowMinimum()
    );
  }
}
