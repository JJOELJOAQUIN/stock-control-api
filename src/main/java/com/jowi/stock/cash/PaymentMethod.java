package com.jowi.stock.cash;

public enum PaymentMethod {
  CASH,
  TRANSFER,
  DEBIT,
  CREDIT;

  public boolean isCard() {
    return this == DEBIT || this == CREDIT;
  }
}

