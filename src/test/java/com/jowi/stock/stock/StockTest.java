package com.jowi.stock.stock;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockTest {

  @Test
  void shouldBeBelowMinimumWhenCurrentIsLessThanMinimum() {
    UUID productId = UUID.randomUUID();

    Stock stock = new Stock(productId, 3, 5);

    assertTrue(stock.isBelowMinimum());
  }

  @Test
  void shouldNotBeBelowMinimumWhenCurrentIsGreaterOrEqualToMinimum() {
    UUID productId = UUID.randomUUID();

    Stock stock = new Stock(productId, 5, 5);

    assertFalse(stock.isBelowMinimum());
  }

  @Test
  void shouldExposeCorrectValues() {
    UUID productId = UUID.randomUUID();

    Stock stock = new Stock(productId, 10, 2);

    assertEquals(productId, stock.getProductId());
    assertEquals(10, stock.getCurrent());
    assertEquals(2, stock.getMinimum());
  }
}