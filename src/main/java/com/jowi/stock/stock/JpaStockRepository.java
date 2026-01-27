package com.jowi.stock.stock;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaStockRepository extends JpaRepository<StockEntity, UUID> {

    long count();

    long countByCurrentGreaterThan(int value);

    long countByCurrentLessThan(int value);

    @Query("""
        SELECT COUNT(s)
        FROM StockEntity s
        WHERE s.current < s.minimum
    """)
    long countBelowMinimum();

    @Query("""
        SELECT s
        FROM StockEntity s
        WHERE s.current < s.minimum
    """)
    List<StockEntity> findBelowMinimum();

    List<StockEntity> findByCurrent(int current);
}
