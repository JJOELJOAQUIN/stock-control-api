package com.jowi.stock.cash;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_movements")
public class CashMovement extends BaseEntity {

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private CashMovementType type;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CashSource source;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashContext context;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount; // monto bruto

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal retention; // retención tarjeta (si aplica)

  @NotNull
  @Column(name = "net_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal netAmount; // amount - retention

  @Column(length = 300)
  private String comment;

  // Relación "lógica" a algo (saleId, expenseId, etc). Opcional.
  @Column(name = "reference_id")
  private java.util.UUID referenceId;

  @Column(name = "doctor_share", precision = 18, scale = 2)
  private BigDecimal doctorShare;

  @Column(name = "cosmetologist_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistShare;

  // ===== getters/setters =====

  public CashMovementType getType() {
    return type;
  }

  public void setType(CashMovementType type) {
    this.type = type;
  }

  public CashSource getSource() {
    return source;
  }

  public void setSource(CashSource source) {
    this.source = source;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public CashContext getContext() {
    return context;
  }

  public void setContext(CashContext context) {
    this.context = context;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public BigDecimal getRetention() {
    return retention;
  }

  public void setRetention(BigDecimal retention) {
    this.retention = retention;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(BigDecimal netAmount) {
    this.netAmount = netAmount;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public java.util.UUID getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(java.util.UUID referenceId) {
    this.referenceId = referenceId;
  }
}
