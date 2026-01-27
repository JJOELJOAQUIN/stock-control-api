package com.jowi.stock.movement;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository
    extends JpaRepository<StockMovement, UUID>,
            JpaSpecificationExecutor<StockMovement> {

  // ===== Listado por producto =====
  Page<StockMovement> findByProductId(UUID productId, Pageable pageable);

  // ===== KPIs =====
  long countByType(StockMovementType type);

long countByCreatedAtAfter(Instant from);

long countByTypeAndCreatedAtAfter(
    StockMovementType type,
    Instant from
);

  @Query("""
    SELECT COALESCE(SUM(m.quantity), 0)
    FROM StockMovement m
    WHERE m.type = :type
  """)
  long sumQuantityByType(StockMovementType type);
}
