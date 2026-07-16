package com.jowi.stock.movement.entities;

import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.common.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Traza qué lote y cuántas unidades participaron de un movimiento de stock.
 *
 * Un movimiento OUT de 5 unidades puede haber salido de 2 lotes distintos
 * (3 del que vence antes + 2 del siguiente): eso son 2 filas acá. Al anular
 * la venta, cada unidad vuelve exactamente al lote del que salió.
 */
@Entity
@Table(name = "stock_movement_batches", indexes = {
    @Index(name = "idx_smb_movement", columnList = "stock_movement_id"),
    @Index(name = "idx_smb_batch", columnList = "batch_id")
})
public class StockMovementBatch extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stock_movement_id", nullable = false)
  private StockMovement stockMovement;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private ProductBatch batch;

  @NotNull
  @Min(1)
  @Column(nullable = false)
  private Integer quantity;

  // ===== getters / setters =====

  public StockMovement getStockMovement() {
    return stockMovement;
  }

  public void setStockMovement(StockMovement stockMovement) {
    this.stockMovement = stockMovement;
  }

  public ProductBatch getBatch() {
    return batch;
  }

  public void setBatch(ProductBatch batch) {
    this.batch = batch;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}