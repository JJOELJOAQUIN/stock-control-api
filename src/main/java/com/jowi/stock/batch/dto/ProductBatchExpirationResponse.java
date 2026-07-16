package com.jowi.stock.batch.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.stock.enums.StockContext;

public record ProductBatchExpirationResponse(
    UUID batchId,
    UUID productId,
    String productName,
    String barcode,
    StockContext context,
    String lotNumber,
    Integer quantityCurrent,
    LocalDate expirationDate,
    Long daysToExpire,
    Boolean estimated
) {
  public static ProductBatchExpirationResponse from(ProductBatch batch) {
    return new ProductBatchExpirationResponse(
        batch.getId(),
        batch.getProduct().getId(),
        batch.getProduct().getName(),
        batch.getProduct().getBarcode(),
        batch.getContext(),
        batch.getLotNumber(),
        batch.getQuantityCurrent(),
        batch.getExpirationDate(),
        ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpirationDate()),
        batch.getExpirationEstimated()
    );
  }
}