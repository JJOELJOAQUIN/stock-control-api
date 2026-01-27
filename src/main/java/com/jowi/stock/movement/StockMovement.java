package com.jowi.stock.movement;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "stock_movements")
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

  /* getters */

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
}
