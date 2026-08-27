package com.jowi.stock.purchase.entities;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Renglón de una orden de compra: qué producto, cuánto y a qué costo.
 *
 * Diseño deliberado (no reusa cash_movement_items): una compra no es una
 * venta. Acá viven datos propios de la compra —costo unitario, lote,
 * vencimiento— que en un ítem de venta no existen. Cuelga del movimiento de
 * caja PROVIDER_PAYMENT que registró el pago de la orden.
 *
 * Relación unidireccional hacia CashMovement: el movimiento de caja no sabe
 * de estos ítems (no queremos que una venta y una compra compartan modelo).
 *
 * El nombre del producto se guarda como snapshot: si mañana se renombra el
 * producto, el detalle histórico de la compra sigue diciendo lo que se
 * compró en su momento.
 */
@Entity
@Table(name = "purchase_items", indexes = {
    @Index(name = "idx_purchase_items_movement", columnList = "cash_movement_id"),
    @Index(name = "idx_purchase_items_product", columnList = "product_id")
})
public class PurchaseItem extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cash_movement_id", nullable = false)
  private CashMovement cashMovement;

  @NotNull
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @NotNull
  @Column(name = "product_name", nullable = false, length = 200)
  private String productName;

  /** Cantidad en ENVASES (igual que se carga la compra). */
  @NotNull
  @Column(nullable = false)
  private Integer quantity;

  /** Costo por envase. */
  @NotNull
  @Column(name = "unit_cost", nullable = false)
  private BigDecimal unitCost;

  /** unitCost * quantity. */
  @NotNull
  @Column(nullable = false)
  private BigDecimal subtotal;

  @Column(name = "lot_number", length = 100)
  private String lotNumber;

  @Column(name = "expiration_date")
  private LocalDate expirationDate;

  public CashMovement getCashMovement() {
    return cashMovement;
  }

  public void setCashMovement(CashMovement cashMovement) {
    this.cashMovement = cashMovement;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public void setLotNumber(String lotNumber) {
    this.lotNumber = lotNumber;
  }

  public LocalDate getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(LocalDate expirationDate) {
    this.expirationDate = expirationDate;
  }
}
