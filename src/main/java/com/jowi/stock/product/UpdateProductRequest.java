package com.jowi.stock.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Min(0) Integer minimumStock,
    @NotNull ProductCategory category,
    @NotNull ProductBrand brand,
    Boolean expirable,
    Boolean active,
    @NotNull BigDecimal costPrice

) {}
