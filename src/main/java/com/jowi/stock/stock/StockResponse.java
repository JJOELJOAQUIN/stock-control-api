package com.jowi.stock.stock;

import java.util.UUID;

public record StockResponse(
    UUID productId,
    int current,
    int minimum,
    boolean belowMinimum
) {

  public static StockResponse from(Stock stock) {
    return new StockResponse(
        stock.getProductId(),
        stock.getCurrent(),
        stock.getMinimum(),
        stock.isBelowMinimum()
    );
  }
}
