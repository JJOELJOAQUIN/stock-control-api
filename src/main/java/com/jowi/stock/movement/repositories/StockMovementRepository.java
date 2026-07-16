package com.jowi.stock.movement.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.stock.enums.StockContext;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, UUID>,
        JpaSpecificationExecutor<StockMovement> {

    // ===== Listado por producto =====
    Page<StockMovement> findByProduct_Id(UUID productId, Pageable pageable);

    // ===== Trazabilidad caja -> stock =====
    // Usado por la anulación para saber qué salidas de stock generó una venta.
    List<StockMovement> findByCashMovementIdOrderByCreatedAtAsc(UUID cashMovementId);

    boolean existsByCashMovementId(UUID cashMovementId);

    // ===== KPIs =====
    long countByType(StockMovementType type);

    long countByCreatedAtAfter(Instant from);

    long countByContext(StockContext context);

    long countByContextAndCreatedAtAfter(
            StockContext context,
            Instant date);

    long countByTypeAndCreatedAtAfter(
            StockMovementType type,
            Instant from);

    @Query("""
              SELECT COALESCE(SUM(m.quantity), 0)
              FROM StockMovement m
              WHERE m.type = :type
            """)
    long sumQuantityByType(StockMovementType type);
}