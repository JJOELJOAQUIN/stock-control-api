package com.jowi.stock.stock.dto;

import java.util.UUID;

import com.jowi.stock.stock.entities.Stock;

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
