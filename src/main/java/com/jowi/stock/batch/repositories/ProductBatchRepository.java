package com.jowi.stock.batch.repositories;

import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.stock.enums.StockContext;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

  List<ProductBatch> findByContextAndExpirationDateBetweenAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
      StockContext context,
      LocalDate from,
      LocalDate to,
      Integer quantityCurrent
  );

  List<ProductBatch> findByProductIdAndContextAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
      UUID productId,
      StockContext context,
      Integer quantityCurrent
  );

  /** Todos los lotes del producto, tengan o no stock. Usado en devoluciones. */
  List<ProductBatch> findByProductIdAndContext(
      UUID productId,
      StockContext context
  );
}
