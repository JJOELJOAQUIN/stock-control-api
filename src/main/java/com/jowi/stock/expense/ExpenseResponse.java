package com.jowi.stock.expense;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public record ExpenseResponse(
    UUID id,
    ExpenseType type,
    ExpenseContext context,
    BigDecimal amount,
    String comment,
    boolean recurring,
    Instant createdAt
) {
  public static ExpenseResponse from(Expense e) {
    return new ExpenseResponse(
        e.getId(),
        e.getType(),
        e.getContext(),
        e.getAmount(),
        e.getComment(),
        e.isRecurring(),
        e.getCreatedAt()
    );
  }
}
