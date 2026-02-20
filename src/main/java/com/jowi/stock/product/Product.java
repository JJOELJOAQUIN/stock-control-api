package com.jowi.stock.product;

import java.math.BigDecimal;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = "barcode")
})
public class Product extends BaseEntity {
  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private Integer minimumStock;

  @Column(nullable = false)
  private Boolean active = true;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProductCategory category;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ProductBrand brand;

  @Column(nullable = false)
  private Boolean expirable = true;

  @Column(unique = true, length = 80)
  private String barcode;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProductScope scope;
  // LOCAL | CONSULTORIO | BOTH

  @Column(precision = 15, scale = 2)
  private BigDecimal costPrice;

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }

  public ProductScope getScope() {
    return scope;
  }

  public void setScope(ProductScope scope) {
    this.scope = scope;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Integer getMinimumStock() {
    return minimumStock;
  }

  public Boolean getActive() {
    return active;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setMinimumStock(Integer minimumStock) {
    this.minimumStock = minimumStock;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public ProductCategory getCategory() {
    return category;
  }

  public void setCategory(ProductCategory category) {
    this.category = category;
  }

  public ProductBrand getBrand() {
    return brand;
  }

  public void setBrand(ProductBrand brand) {
    this.brand = brand;
  }

  public Boolean getExpirable() {
    return expirable;
  }

  public void setExpirable(Boolean expirable) {
    this.expirable = expirable;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }
}
