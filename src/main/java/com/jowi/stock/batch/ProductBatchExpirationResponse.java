package com.jowi.stock.batch;

import com.jowi.stock.stock.StockContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record ProductBatchExpirationResponse(
    UUID batchId,
    UUID productId,
    String productName,
    String barcode,
    StockContext context,
    String lotNumber,
    Integer quantityCurrent,
    LocalDate expirationDate,
    Long daysToExpire
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
        ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpirationDate())
    );
  }
}
