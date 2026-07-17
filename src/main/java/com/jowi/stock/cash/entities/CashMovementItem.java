package com.jowi.stock.cash.entities;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashMovementItemKind;
import com.jowi.stock.cash.enums.SplitPreset;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cash_movement_items", indexes = {
    @Index(name = "idx_cmi_movement", columnList = "cash_movement_id"),
    @Index(name = "idx_cmi_product", columnList = "product_id"),
    @Index(name = "idx_cmi_performed_by", columnList = "performed_by")
})
public class CashMovementItem extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cash_movement_id", nullable = false)
  private CashMovement cashMovement;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashMovementItemKind kind;

  // Presente sólo para kind = PRODUCT.
  @Column(name = "product_id")
  private UUID productId;

  // Presente sólo para kind = PROCEDURE.
  @Column(name = "procedure_code", length = 60)
  private String procedureCode;

  @NotNull
  @Column(nullable = false, length = 200)
  private String description;

  @NotNull
  @Column(nullable = false)
  private Integer quantity;

  @NotNull
  @Column(name = "unit_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal unitAmount;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal subtotal;

  /**
   * Quién hizo el trabajo (MEDICA / COSMETOLOGA).
   *
   * Antes este dato entraba por el request, se usaba para calcular los
   * shares y se perdía: el sistema sabía cuánta plata le tocó a cada una,
   * pero no quién había hecho el trabajo. Eso obligaba a inferir la autoría
   * desde el monto ("si la cosmetóloga cobró algo, lo hizo ella"), lo cual
   * se rompe en cuanto alguien cobra 0% de algo que sí hizo.
   *
   * Nullable porque los ítems anteriores a esta versión no lo tienen: para
   * ellos se hace backfill donde se puede inferir sin ambigüedad, y quedan
   * en NULL donde no (procedimientos con 0% para la cosmetóloga).
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "performed_by", length = 20)
  private CashActor performedBy;

  /**
   * Reparto aplicado, sólo para el protocolo de peeling profundo. NULL en
   * todo lo demás: los ítems PRODUCT no lo usan y el resto del catálogo de
   * procedimientos no admite desvíos.
   *
   * Vale NORMAL cuando se cobró como corresponde. Los otros valores marcan
   * un desvío deliberado, y son la única traza de que hubo una deuda entre
   * Pili y Gise saldándose adentro de ese pago.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "split_preset", length = 20)
  private SplitPreset splitPreset;

  @Column(name = "doctor_share", precision = 18, scale = 2)
  private BigDecimal doctorShare;

  @Column(name = "cosmetologist_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistShare;

  // ===== getters/setters =====

  public CashMovement getCashMovement() {
    return cashMovement;
  }

  public void setCashMovement(CashMovement cashMovement) {
    this.cashMovement = cashMovement;
  }

  public CashMovementItemKind getKind() {
    return kind;
  }

  public void setKind(CashMovementItemKind kind) {
    this.kind = kind;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public String getProcedureCode() {
    return procedureCode;
  }

  public void setProcedureCode(String procedureCode) {
    this.procedureCode = procedureCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitAmount() {
    return unitAmount;
  }

  public void setUnitAmount(BigDecimal unitAmount) {
    this.unitAmount = unitAmount;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public CashActor getPerformedBy() {
    return performedBy;
  }

  public void setPerformedBy(CashActor performedBy) {
    this.performedBy = performedBy;
  }

  public SplitPreset getSplitPreset() {
    return splitPreset;
  }

  public void setSplitPreset(SplitPreset splitPreset) {
    this.splitPreset = splitPreset;
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
}