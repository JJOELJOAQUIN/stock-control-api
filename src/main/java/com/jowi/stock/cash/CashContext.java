package com.jowi.stock.cash;

import com.jowi.stock.stock.StockContext;

public enum CashContext {
  LOCAL, CONSULTORIO;

  public StockContext toStockContext() {
    return StockContext.valueOf(this.name());
  }
}