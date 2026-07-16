package com.jowi.stock.cash.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.PaymentMethod;

public interface CashMovementRepository
    extends JpaRepository<CashMovement, UUID>, JpaSpecificationExecutor<CashMovement> {
  Page<CashMovement> findByContext(CashContext context, Pageable pageable);

  @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CashMovement c
        WHERE c.type = 'IN'
      """)
  BigDecimal totalIn();

  @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CashMovement c
        WHERE c.type = 'OUT'
      """)
  BigDecimal totalOut();

  @Query("""
        SELECT COALESCE(SUM(c.retention), 0)
        FROM CashMovement c
      """)
  BigDecimal totalRetention();

  @Query("""
        SELECT COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE c.context = :context
      """)
  BigDecimal netByContext(CashContext context);

  @Query("""
        SELECT COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE c.paymentMethod = :method
      """)
  BigDecimal netByPayment(PaymentMethod method);

  @Query("""
          SELECT COALESCE(SUM(c.amount), 0)
          FROM CashMovement c
          WHERE c.type = :type
            AND c.context = :context
      """)
  BigDecimal sumAmountByTypeAndContext(CashMovementType type, CashContext context);

  @Query("""
          SELECT COALESCE(SUM(c.retention), 0)
          FROM CashMovement c
          WHERE c.context = :context
      """)
  BigDecimal sumRetentionByContext(CashContext context);

  @Query("""
          SELECT COALESCE(SUM(c.amount), 0)
          FROM CashMovement c
          WHERE c.type = :type
            AND c.paymentMethod = :method
      """)
  BigDecimal sumAmountByTypeAndPaymentMethod(
      CashMovementType type,
      PaymentMethod method);

  @Query("""
          SELECT COALESCE(SUM(c.retention), 0)
          FROM CashMovement c
          WHERE c.paymentMethod = :method
      """)
  BigDecimal sumRetentionByPaymentMethod(PaymentMethod method);

  @Query("""
        SELECT
          MONTH(c.createdAt),
          SUM(CASE WHEN c.type = 'IN'  THEN c.amount ELSE 0 END),
          SUM(CASE WHEN c.type = 'OUT' THEN c.amount ELSE 0 END),
          SUM(c.retention),
          SUM(c.netAmount)
        FROM CashMovement c
        WHERE YEAR(c.createdAt) = :year
          AND (:context IS NULL OR c.context = :context)
        GROUP BY MONTH(c.createdAt)
        ORDER BY MONTH(c.createdAt)
      """)
  List<Object[]> aggregateMonthly(
      @Param("year") int year,
      @Param("context") CashContext context);

  @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN c.type = 'IN'  THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.type = 'OUT' THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(c.retention), 0),
          COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE YEAR(c.createdAt) = :year
          AND MONTH(c.createdAt) = :month
          AND (:context IS NULL OR c.context = :context)
      """)
  Object[] aggregateMonth(
      @Param("year") int year,
      @Param("month") int month,
      @Param("context") CashContext context);

  @Query("""
          SELECT
            COALESCE(SUM(c.netAmount), 0),
            COALESCE(SUM(c.doctorShare), 0),
            COALESCE(SUM(c.cosmetologistShare), 0)
          FROM CashMovement c
          WHERE c.type = 'IN'
            AND c.context = :context
            AND YEAR(c.createdAt) = :year
            AND MONTH(c.createdAt) = :month
      """)
  Object[] cashSplitByContextAndMonth(
      @Param("context") CashContext context,
      @Param("year") int year,
      @Param("month") int month);

  @Query("""
        SELECT
          COALESCE(SUM(c.netAmount), 0),
          COALESCE(SUM(c.doctorShare), 0),
          COALESCE(SUM(c.cosmetologistShare), 0)
        FROM CashMovement c
        WHERE c.type = 'IN'
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  Object[] cashSplitByContextAndDateRange(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);

 /**
   * Producción de la COSMETÓLOGA por ítem.
   *
   * El filtro i.cosmetologistShare > 0 es el que define "trabajo de la
   * cosmetóloga" y NO es opcional: sin él, los ítems propios de la médica
   * (venta con performedBy = MEDICA, procedimiento 100% médica) entran con
   * doctorShare = neto completo y la card de la cosmetóloga termina
   * exponiendo el día entero de la médica.
   *
   * El filtro va a nivel ÍTEM y no de movimiento porque una venta combinada
   * puede mezclar un procedimiento de Gise con una venta de Pili en el mismo
   * CashMovement: filtrar por la cabecera arrastraría los ítems de la médica.
   */

  @Query("""
        SELECT
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE
              THEN i.cosmetologistShare ELSE 0 END), 0),
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE
              THEN i.doctorShare ELSE 0 END), 0),
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PRODUCT
              THEN i.cosmetologistShare ELSE 0 END), 0),
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PRODUCT
              THEN i.doctorShare ELSE 0 END), 0)
        FROM CashMovement c
        JOIN c.items i
        WHERE c.type = 'IN'
          AND c.context = :context
          AND i.cosmetologistShare > 0
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  Object[] cosmetologistProductionSplitFromItems(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);

  @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN c.source = 'PROCEDURE'    THEN c.cosmetologistShare ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.source = 'PROCEDURE'    THEN c.doctorShare        ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.source = 'PRODUCT_SALE' THEN c.cosmetologistShare ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.source = 'PRODUCT_SALE' THEN c.doctorShare        ELSE 0 END), 0)
        FROM CashMovement c
        WHERE c.type = 'IN'
          AND c.context = :context
          AND c.cosmetologistShare > 0
          AND c.items IS EMPTY
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  Object[] cosmetologistProductionSplitLegacy(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);

  // ===================================================================
  // Totales de venta (producto / procedimiento). Mismo esquema híbrido.
  // ===================================================================

  @Query("""
        SELECT
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PRODUCT
              THEN i.subtotal ELSE 0 END), 0),
          COALESCE(SUM(CASE
            WHEN i.kind = com.jowi.stock.cash.enums.CashMovementItemKind.PROCEDURE
              THEN i.subtotal ELSE 0 END), 0)
        FROM CashMovement c
        JOIN c.items i
        WHERE c.type = 'IN'
          AND c.context = :context
      """)
  Object[] salesTotalsFromItems(@Param("context") CashContext context);

  @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN c.source = 'PRODUCT_SALE' THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.source = 'PROCEDURE'    THEN c.amount ELSE 0 END), 0)
        FROM CashMovement c
        WHERE c.type = 'IN'
          AND c.context = :context
          AND c.items IS EMPTY
      """)
  Object[] salesTotalsLegacy(@Param("context") CashContext context);
}