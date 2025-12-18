package com.jowi.stock.product;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

  @NotBlank
  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  @NotNull
  @Min(0)
  @Column(nullable = false)
  private Integer minimumStock;

  @Column(nullable = false)
  private Boolean active = true;

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
}
