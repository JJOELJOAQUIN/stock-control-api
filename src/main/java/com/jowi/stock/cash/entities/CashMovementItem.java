package com.jowi.stock.cash.entities;

import com.jowi.stock.cash.enums.CashMovementItemKind;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cash_movement_items", indexes = {
    @Index(name = "idx_cmi_movement", columnList = "cash_movement_id"),
    @Index(name = "idx_cmi_product", columnList = "product_id")
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