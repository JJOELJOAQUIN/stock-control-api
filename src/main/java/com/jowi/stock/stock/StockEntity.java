package com.jowi.stock.stock;

import com.jowi.stock.product.Product;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "product_id", "context" })
}, indexes = {
    @Index(name = "idx_stocks_product_context", columnList = "product_id, context")
})
public class StockEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StockContext context;

  @Column(name = "current_stock", nullable = false)
  private int current;

  @Version
  @Column(nullable = false)
  private Long version;

  protected StockEntity() {
  }

  public StockEntity(Product product, StockContext context, int current) {
    this.product = product;
    this.context = context;
    this.current = current;
  }

  public UUID getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public StockContext getContext() {
    return context;
  }

  public int getCurrent() {
    return current;
  }

  public void setCurrent(int current) {
    if (current < 0) {
      throw new IllegalArgumentException("Stock cannot be negative");
    }
    this.current = current;
  }
}
