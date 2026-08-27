package com.jowi.stock.product.dto;

import java.math.BigDecimal;

import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;

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
        @NotNull BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal defaultMarkupPercentage,
        @Min(1) Integer shelfLifeMonths,
        @Min(0) Integer restockPriority) {
}
