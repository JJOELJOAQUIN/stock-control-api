package com.jowi.stock.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Min(0) Integer minimumStock,
    @NotNull ProductCategory category,
    @NotNull ProductBrand brand,
    Boolean expirable,
    @NotNull ProductScope scope,
    String barcode
     
) {}
