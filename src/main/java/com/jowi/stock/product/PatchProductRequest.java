package com.jowi.stock.product;

public record PatchProductRequest(
    String name,
    String description,
    Integer minimumStock,
    ProductCategory category,
    ProductBrand brand,
    Boolean expirable,
    Boolean active
) {}
