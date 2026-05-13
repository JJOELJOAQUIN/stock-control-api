package com.jowi.stock.product.dto;

import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;

public record PatchProductRequest(
    String name,
    String description,
    Integer minimumStock,
    ProductCategory category,
    ProductBrand brand,
    Boolean expirable,
    Boolean active
) {}
