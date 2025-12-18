package com.jowi.stock.movement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, UUID> {

  @Query("""
    SELECT COALESCE(SUM(
      CASE m.type
        WHEN 'IN' THEN m.quantity
        WHEN 'OUT' THEN -m.quantity
        ELSE m.quantity
      END
    ), 0)
    FROM StockMovement m
    WHERE m.product.id = :productId
  """)
  Integer calculateCurrentStock(@Param("productId") UUID productId);
}
