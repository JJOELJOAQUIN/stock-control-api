package com.jowi.stock.cash.enums;

import com.jowi.stock.stock.enums.StockContext;

public enum CashContext {
  LOCAL, CONSULTORIO;

  public StockContext toStockContext() {
    return StockContext.valueOf(this.name());
  }
}