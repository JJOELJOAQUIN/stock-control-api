package com.jowi.stock.product.entities;

import java.math.BigDecimal;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;
import com.jowi.stock.product.enums.ProductScope;

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

  @Column(name = "sale_price", precision = 15, scale = 2)
  private BigDecimal salePrice;

  @Column(name = "default_markup_percentage", precision = 5, scale = 2)
  private BigDecimal defaultMarkupPercentage;

  /**
   * Vida útil estimada en meses para productos SIN fecha de vencimiento
   * impresa (magistrales). Si está configurada y en la compra no se carga
   * vencimiento manual, el sistema calcula la fecha estimada desde la fecha
   * de ingreso. Null = sin estimación automática.
   *
   * Ejemplos: cremas/emulsiones 6, serums 8, espumas/limpieza 8,
   * Vitamina C Plus 1, Labial Vitamina E pura 3.
   */
  @Column(name = "shelf_life_months")
  private Integer shelfLifeMonths;

  /**
   * Prioridad de reposición para el aviso de stock bajo.
   * 0 = normal, 1 = alta, 2 = crítica.
   */
  @Column(name = "restock_priority", nullable = false)
  private Integer restockPriority = 0;

  public Integer getShelfLifeMonths() {
    return shelfLifeMonths;
  }

  public void setShelfLifeMonths(Integer shelfLifeMonths) {
    this.shelfLifeMonths = shelfLifeMonths;
  }

  public Integer getRestockPriority() {
    return restockPriority;
  }

  public void setRestockPriority(Integer restockPriority) {
    this.restockPriority = restockPriority == null ? 0 : restockPriority;
  }

  public BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(BigDecimal salePrice) {
    this.salePrice = salePrice;
  }

  public BigDecimal getDefaultMarkupPercentage() {
    return defaultMarkupPercentage;
  }

  public void setDefaultMarkupPercentage(BigDecimal defaultMarkupPercentage) {
    this.defaultMarkupPercentage = defaultMarkupPercentage;
  }

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