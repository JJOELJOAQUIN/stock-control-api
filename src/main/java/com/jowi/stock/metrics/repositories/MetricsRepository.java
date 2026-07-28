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
   * Procedimientos del mes, contados SIEMPRE desde los ítems. Cubre los dos
   * orígenes: el flujo directo (source = PROCEDURE, un ítem espejo por
   * movimiento) y la venta combinada (source = COMBINED_SALE, un ítem por
   * línea). Un solo camino, sin doble conteo.
   *
   * La cantidad sale de SUM(i.quantity): así una consulta cargada con
   * cantidad 3 cuenta como 3, no como 1. Esto reemplazó al COUNT(c) sobre
   * cabeceras, que ignoraba la cantidad y hacía que la métrica dijera 15
   * cuando la facturación correspondía a 19.
   *
   * El neto por ítem no se persiste; se informa el subtotal en su lugar
   * (en consultorio la retención suele ser 0, así que bruto ≈ neto).
   *
   * Precondición: todo procedimiento no anulado tiene exactamente un ítem
   * de kind PROCEDURE con su procedureCode. Lo garantizan el ítem espejo de
   * CashMovementService y el saneamiento de los datos históricos.
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
          AND c.source IN ('PROCEDURE', 'COMBINED_SALE')
          AND i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE
          AND i.procedureCode IS NOT NULL
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
        GROUP BY i.procedureCode
      """)
  List<Object[]> proceduresByItems(
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