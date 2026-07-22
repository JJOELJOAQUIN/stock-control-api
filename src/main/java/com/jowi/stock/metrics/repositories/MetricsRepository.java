package com.jowi.stock.metrics.repositories;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Queries de métricas mensuales. Separado de CashMovementRepository a
 * propósito: aquel archivo tiene 17 agregaciones donde un filtro olvidado
 * hace mentir la caja, y no conviene seguir tocándolo.
 *
 * Todas filtran voided = false. Devuelven filas crudas por procedure_code:
 * la clasificación en médica/cosmetología la hace el front con el catálogo
 * que ya tiene, en vez de duplicarlo acá.
 */
public interface MetricsRepository extends Repository<CashMovement, UUID> {

  /**
   * Procedimientos cobrados por su propio flujo (source = PROCEDURE).
   *
   * OJO con el conteo: cuando se cargan 2 consultas juntas, el sistema graba
   * UN movimiento con el monto duplicado y "×2" en el comentario. Acá cuenta
   * como 1. El facturado es exacto; la cantidad es piso, no exacta.
   */
  @Query("""
        SELECT c.procedureCode,
               COUNT(c),
               COALESCE(SUM(c.amount), 0),
               COALESCE(SUM(c.netAmount), 0),
               COALESCE(SUM(c.doctorShare), 0),
               COALESCE(SUM(c.cosmetologistShare), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.source = 'PROCEDURE'
          AND c.procedureCode IS NOT NULL
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
        GROUP BY c.procedureCode
      """)
  List<Object[]> proceduresFromHeader(
      @Param("context") CashContext context,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Procedimientos que fueron parte de una venta combinada. El neto por ítem
   * no se persiste, así que se informa el subtotal en su lugar.
   */
  @Query("""
        SELECT i.procedureCode,
               COALESCE(SUM(i.quantity), 0),
               COALESCE(SUM(i.subtotal), 0),
               COALESCE(SUM(i.subtotal), 0),
               COALESCE(SUM(i.doctorShare), 0),
               COALESCE(SUM(i.cosmetologistShare), 0)
        FROM CashMovement c
        JOIN c.items i
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.source = 'COMBINED_SALE'
          AND i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE
          AND i.procedureCode IS NOT NULL
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
        GROUP BY i.procedureCode
      """)
  List<Object[]> proceduresFromItems(
      @Param("context") CashContext context,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Ventas de producto por su propio flujo. */
  @Query("""
        SELECT COUNT(c),
               COALESCE(SUM(c.amount), 0),
               COALESCE(SUM(c.netAmount), 0),
               COALESCE(SUM(c.doctorShare), 0),
               COALESCE(SUM(c.cosmetologistShare), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.source = 'PRODUCT_SALE'
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  List<Object[]> productsFromHeader(
      @Param("context") CashContext context,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Productos vendidos dentro de una venta combinada. */
  @Query("""
        SELECT COALESCE(SUM(i.quantity), 0),
               COALESCE(SUM(i.subtotal), 0),
               COALESCE(SUM(i.subtotal), 0),
               COALESCE(SUM(i.doctorShare), 0),
               COALESCE(SUM(i.cosmetologistShare), 0)
        FROM CashMovement c
        JOIN c.items i
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.source = 'COMBINED_SALE'
          AND i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PRODUCT
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  List<Object[]> productsFromItems(
      @Param("context") CashContext context,
      @Param("from") Instant from,
      @Param("to") Instant to);
}