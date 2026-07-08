package com.jowi.stock.treatment.entities;

import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "treatment_payments", indexes = {
    @Index(name = "idx_payment_treatment", columnList = "treatment_id")
})
public class Payment extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "treatment_id", nullable = false)
  private Treatment treatment;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  // 1 = primer pago (lleva el share de la cosmetóloga), 2 = segundo, etc.
  @NotNull
  @Column(name = "installment_number", nullable = false)
  private Integer installmentNumber;

  // Vínculo al CashMovement que generó este pago (para que caja/split funcionen).
  @Column(name = "cash_movement_id")
  private UUID cashMovementId;

  public Treatment getTreatment() { return treatment; }
  public void setTreatment(Treatment treatment) { this.treatment = treatment; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public PaymentMethod getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

  public Integer getInstallmentNumber() { return installmentNumber; }
  public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }

  public UUID getCashMovementId() { return cashMovementId; }
  public void setCashMovementId(UUID cashMovementId) { this.cashMovementId = cashMovementId; }
}