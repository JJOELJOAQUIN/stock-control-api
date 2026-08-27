package com.jowi.stock.movement.entities;

import java.util.UUID;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.stock.enums.StockContext;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movements_product_context", columnList = "product_id, context"),
    @Index(name = "idx_stock_movements_type", columnList = "type"),
    @Index(name = "idx_stock_movements_cash_movement", columnList = "cash_movement_id")
})

public class StockMovement extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StockMovementType type;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private StockMovementReason reasonType;

  @NotNull
  @Min(1)
  @Column(nullable = false)
  private Integer quantity;

  @Column(length = 300)
  private String comment;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StockContext context;

  /**
   * Movimiento de caja que originó esta salida/entrada de stock, cuando
   * existe. Es un UUID suelto y no una @ManyToOne a propósito: el módulo de
   * stock no depende del de caja, y un movimiento de stock puede no tener
   * caja detrás (consumo interno, ajuste, stock inicial).
   *
   * Nulo en todos los movimientos anteriores a esta versión: por eso la
   * anulación tiene rama legacy que reconstruye desde cash_movement_items.
   */
  @Column(name = "cash_movement_id")
  private UUID cashMovementId;

  /* getters */

  public StockContext getContext() {
    return context;
  }

  public Product getProduct() {
    return product;
  }

  public StockMovementType getType() {
    return type;
  }

  public StockMovementReason getReasonType() {
    return reasonType;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public String getComment() {
    return comment;
  }

  public UUID getCashMovementId() {
    return cashMovementId;
  }

  /* setters */

  public void setProduct(Product product) {
    this.product = product;
  }

  public void setType(StockMovementType type) {
    this.type = type;
  }

  public void setReasonType(StockMovementReason reasonType) {
    this.reasonType = reasonType;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public void setContext(StockContext context) {
    this.context = context;
  }

  public void setCashMovementId(UUID cashMovementId) {
    this.cashMovementId = cashMovementId;
  }
}
