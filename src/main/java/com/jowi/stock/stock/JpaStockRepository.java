package com.jowi.stock.stock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStockRepository extends JpaRepository<StockEntity, UUID> {

    Optional<StockEntity> findByProduct_IdAndContext(
            UUID productId,
            StockContext context);

    boolean existsByProduct_IdAndContext(
            UUID productId,
            StockContext context);

    List<StockEntity> findByContext(StockContext context);
}
