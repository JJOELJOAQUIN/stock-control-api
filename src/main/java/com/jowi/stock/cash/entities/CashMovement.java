package com.jowi.stock.cash.entities;

import java.util.ArrayList;
import java.util.List;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cash_movements", indexes = {
    @Index(name = "idx_cash_context", columnList = "context"),
    @Index(name = "idx_cash_created_at", columnList = "createdAt")
})

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

  @Column(length = 200)
  private String detail;

  // Relación "lógica" a algo (saleId, expenseId, etc). Opcional.
  @Column(name = "reference_id")
  private java.util.UUID referenceId;

  @Column(name = "doctor_share", precision = 18, scale = 2)
  private BigDecimal doctorShare;

  @Column(name = "cosmetologist_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistShare;

  // ===== Anulación (soft delete visible) =====
  // El movimiento anulado NO se borra: queda en la tabla, tachado, con fecha,
  // motivo y quién lo anuló. Desaparece de todas las agregaciones (caja del
  // día, splits, totales) pero no de la historia — el listado lo sigue
  // mostrando, que es lo que diferencia esto de un delete.
  //
  // Los montos NO se tocan al anular: quedan tal cual se registraron. Anular
  // dos veces no se puede; des-anular tampoco — si la anulación fue un error,
  // se vuelve a cargar el movimiento.

  @Column(nullable = false)
  private boolean voided = false;

  @Column(name = "voided_at")
  private Instant voidedAt;

  @Column(name = "void_reason", length = 300)
  private String voidReason;

  /** Email de quien anuló, resuelto por CurrentUserService. */
  @Column(name = "voided_by", length = 120)
  private String voidedBy;

  @Column(name = "procedure_code", length = 60)
  private String procedureCode;

  @OneToMany(mappedBy = "cashMovement", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CashMovementItem> items = new ArrayList<>();

  /** Agrega un ítem de detalle y setea el lado dueño de la relación. */
  public void addItem(CashMovementItem item) {
    item.setCashMovement(this);
    this.items.add(item);
  }

  public List<CashMovementItem> getItems() {
    return items;
  }

  public void setItems(List<CashMovementItem> items) {
    this.items = items;
  }

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

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public java.util.UUID getReferenceId() {
    return referenceId;
  }

  public String getProcedureCode() {
    return procedureCode;
  }

  public void setProcedureCode(String procedureCode) {
    this.procedureCode = procedureCode;
  }

  public void setReferenceId(java.util.UUID referenceId) {
    this.referenceId = referenceId;
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

  public boolean isVoided() {
    return voided;
  }

  public void setVoided(boolean voided) {
    this.voided = voided;
  }

  public Instant getVoidedAt() {
    return voidedAt;
  }

  public void setVoidedAt(Instant voidedAt) {
    this.voidedAt = voidedAt;
  }

  public String getVoidReason() {
    return voidReason;
  }

  public void setVoidReason(String voidReason) {
    this.voidReason = voidReason;
  }

  public String getVoidedBy() {
    return voidedBy;
  }

  public void setVoidedBy(String voidedBy) {
    this.voidedBy = voidedBy;
  }
}
