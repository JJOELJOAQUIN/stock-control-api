package com.jowi.stock.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "stocks")
public class StockEntity {

  @Id
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "current_stock", nullable = false)
  private int current;

  @Column(name = "minimum_stock", nullable = false)
  private int minimum;

  protected StockEntity() {}

  public StockEntity(UUID productId, int current, int minimum) {
    this.productId = productId;
    this.current = current;
    this.minimum = minimum;
  }

  public UUID getProductId() {
    return productId;
  }

  public int getCurrent() {
    return current;
  }

  public int getMinimum() {
    return minimum;
  }

  public void setCurrent(int current) {
    this.current = current;
  }

  public void setMinimum(int minimum) {
    this.minimum = minimum;
  }
}
