package com.jowi.stock.batch.entities;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.stock.enums.StockContext;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(
    name = "product_batches",
    indexes = {
        @Index(name = "idx_product_batches_product_context", columnList = "product_id, context"),
        @Index(name = "idx_product_batches_expiration_date", columnList = "expiration_date")
    }
)
public class ProductBatch extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StockContext context;

  @Column(name = "lot_number", length = 80)
  private String lotNumber;

  @NotNull
  @Column(name = "quantity_initial", nullable = false)
  private Integer quantityInitial;

  @NotNull
  @Column(name = "quantity_current", nullable = false)
  private Integer quantityCurrent;

  @Column(name = "expiration_date")
  private LocalDate expirationDate;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public StockContext getContext() {
    return context;
  }

  public void setContext(StockContext context) {
    this.context = context;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public void setLotNumber(String lotNumber) {
    this.lotNumber = lotNumber;
  }

  public Integer getQuantityInitial() {
    return quantityInitial;
  }

  public void setQuantityInitial(Integer quantityInitial) {
    this.quantityInitial = quantityInitial;
  }

  public Integer getQuantityCurrent() {
    return quantityCurrent;
  }

  public void setQuantityCurrent(Integer quantityCurrent) {
    this.quantityCurrent = quantityCurrent;
  }

  public LocalDate getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(LocalDate expirationDate) {
    this.expirationDate = expirationDate;
  }
}