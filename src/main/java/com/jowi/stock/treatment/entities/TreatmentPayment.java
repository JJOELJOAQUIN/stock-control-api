package com.jowi.stock.treatment.entities;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Pago de un tratamiento. Cada pago genera UN CashMovement (el dinero entra
 * a caja una sola vez). cashMovementId guarda ese vínculo: la fuente de
 * verdad del dinero es CashMovement; este registro solo lo contextualiza.
 */
@Entity
@Table(name = "treatment_payments", indexes = {
    @Index(name = "idx_treatment_payment_treatment", columnList = "treatment_id")
})
public class TreatmentPayment extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "treatment_id", nullable = false)
  private Treatment treatment;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false, length = 20)
  private PaymentMethod paymentMethod;

  // Vínculo al movimiento de caja generado por este pago.
  @Column(name = "cash_movement_id")
  private UUID cashMovementId;

  // Reparto de este pago (la cosmetóloga cobra su parte fija en el primer pago).
  @Column(name = "doctor_share", precision = 18, scale = 2)
  private BigDecimal doctorShare;

  @Column(name = "cosmetologist_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistShare;

  @Column(length = 300)
  private String comment;

  // ===== getters/setters =====

  public Treatment getTreatment() {
    return treatment;
  }

  public void setTreatment(Treatment treatment) {
    this.treatment = treatment;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public UUID getCashMovementId() {
    return cashMovementId;
  }

  public void setCashMovementId(UUID cashMovementId) {
    this.cashMovementId = cashMovementId;
  }

  public BigDecimal getDoctorShare() {
    return doctorShare;
  }

  public void setDoctorShare(BigDecimal doctorShare) {
    this.doctorShare = doctorShare;
  }

  public BigDecimal getCosmetologistShare() {
    return cosmetologistShare;
  }

  public void setCosmetologistShare(BigDecimal cosmetologistShare) {
    this.cosmetologistShare = cosmetologistShare;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
