package com.jowi.stock.stock.dto;

import java.util.UUID;

import com.jowi.stock.stock.entities.Stock;

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
