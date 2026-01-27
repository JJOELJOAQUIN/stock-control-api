package com.jowi.stock.expense;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExpenseType type;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExpenseContext context;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(length = 300)
  private String comment;

  @Column(nullable = false)
  private boolean recurring = false;

  // getters/setters

  public ExpenseType getType() {
    return type;
  }

  public void setType(ExpenseType type) {
    this.type = type;
  }

  public ExpenseContext getContext() {
    return context;
  }

  public void setContext(ExpenseContext context) {
    this.context = context;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isRecurring() {
    return recurring;
  }

  public void setRecurring(boolean recurring) {
    this.recurring = recurring;
  }
}
