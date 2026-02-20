package com.jowi.stock.product;

import java.util.UUID;

public record ProductScanResponse(
    UUID id,
    String name,
    String barcode,
    String category,
    String brand,
    Boolean expirable
) {
  public static ProductScanResponse from(Product p) {
    return new ProductScanResponse(
        p.getId(),
        p.getName(),
        p.getBarcode(),
        p.getCategory().name(),
        p.getBrand().name(),
        p.getExpirable()
    );
  }
}