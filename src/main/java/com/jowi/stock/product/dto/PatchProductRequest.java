package com.jowi.stock.product.dto;

import java.math.BigDecimal;

import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;

public record PatchProductRequest(
    String name,
    String description,
    Integer minimumStock,
    ProductCategory category,
    ProductBrand brand,
    Boolean expirable,
    Boolean active,
    BigDecimal costPrice,
    BigDecimal salePrice,
    BigDecimal defaultMarkupPercentage,
    Integer shelfLifeMonths,
    Integer restockPriority
) {}