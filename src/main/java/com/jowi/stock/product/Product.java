package com.jowi.stock.product;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "products")
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

}
