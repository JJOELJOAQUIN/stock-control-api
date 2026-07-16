package com.jowi.stock.movement.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jowi.stock.movement.entities.StockMovementBatch;

public interface StockMovementBatchRepository
    extends JpaRepository<StockMovementBatch, UUID> {

  List<StockMovementBatch> findByStockMovement_Id(UUID stockMovementId);

  List<StockMovementBatch> findByStockMovement_IdIn(List<UUID> stockMovementIds);
}