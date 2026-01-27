package com.jowi.stock.stock;

import java.util.UUID;

public class Stock {

  private final UUID productId;
  private final int current;
  private final int minimum;
  private final boolean belowMinimum;

  public Stock(UUID productId, int current, int minimum) {
    this.productId = productId;
    this.current = current;
    this.minimum = minimum;
    this.belowMinimum = current < minimum;
  }

  public UUID getProductId() { 
    return productId;
  }

  public int getCurrent() {
    return current;
  }

  public int getMinimum() {
    return minimum;
  }

  public boolean isBelowMinimum() {
    return belowMinimum;
  }
}
