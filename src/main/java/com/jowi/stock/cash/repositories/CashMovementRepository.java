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

/**
 * IMPORTANTE — anulación: TODA query de agregación filtra c.voided = false.
 * Un movimiento anulado no suma en ningún total, split ni métrica. Si se
 * agrega una query con SUM() acá, tiene que llevar el filtro; olvidarlo hace
 * que la caja del día mienta sin dar ningún error.
 *
 * Las excepciones son deliberadas: findByContext y las Specifications del
 * listado NO filtran, porque la tabla tiene que seguir mostrando los
 * anulados tachados. Anular no es borrar.
 */
public interface CashMovementRepository
    extends JpaRepository<CashMovement, UUID>, JpaSpecificationExecutor<CashMovement> {
  Page<CashMovement> findByContext(CashContext context, Pageable pageable);

  @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.type = 'IN'
      """)
  BigDecimal totalIn();

  @Query("""
        SELECT COALESCE(SUM(c.amount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.type = 'OUT'
      """)
  BigDecimal totalOut();

  @Query("""
        SELECT COALESCE(SUM(c.retention), 0)
        FROM CashMovement c
        WHERE c.voided = false
      """)
  BigDecimal totalRetention();

  @Query("""
        SELECT COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.context = :context
      """)
  BigDecimal netByContext(CashContext context);

  @Query("""
        SELECT COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.paymentMethod = :method
      """)
  BigDecimal netByPayment(PaymentMethod method);

  @Query("""
          SELECT COALESCE(SUM(c.amount), 0)
          FROM CashMovement c
          WHERE c.voided = false
            AND c.type = :type
            AND c.context = :context
      """)
  BigDecimal sumAmountByTypeAndContext(CashMovementType type, CashContext context);

  @Query("""
          SELECT COALESCE(SUM(c.retention), 0)
          FROM CashMovement c
          WHERE c.voided = false
            AND c.context = :context
      """)
  BigDecimal sumRetentionByContext(CashContext context);

  @Query("""
          SELECT COALESCE(SUM(c.amount), 0)
          FROM CashMovement c
          WHERE c.voided = false
            AND c.type = :type
            AND c.paymentMethod = :method
      """)
  BigDecimal sumAmountByTypeAndPaymentMethod(
      CashMovementType type,
      PaymentMethod method);

  @Query("""
          SELECT COALESCE(SUM(c.retention), 0)
          FROM CashMovement c
          WHERE c.voided = false
            AND c.paymentMethod = :method
      """)
  BigDecimal sumRetentionByPaymentMethod(PaymentMethod method);



  /**
   * Agregado de caja (ingresos, egresos, retención, neto) para un rango
   * [from, to) en tiempo absoluto. Reemplaza a las agregaciones que filtraban
   * con YEAR()/MONTH() sobre createdAt: esas corrían en el timezone de la
   * sesión de Postgres, así que un cobro de la noche AR caía en el mes UTC
   * siguiente. El rango se arma en hora Argentina desde el service
   * (BusinessZone) y acá sólo se compara contra el Instant, timezone-safe.
   */
  @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN c.type = 'IN'  THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.type = 'OUT' THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(c.retention), 0),
          COALESCE(SUM(c.netAmount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND (:context IS NULL OR c.context = :context)
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  Object[] aggregateRange(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);

  // NOTA: sin uso en el código y NO timezone-safe (YEAR/MONTH corren en la
  // zona de la sesión de Postgres). Si se necesita, migrar a aggregateRange
  // con BusinessZone.ofMonth como el resto. Se deja para no ampliar el diff.
  @Query("""
          SELECT
            COALESCE(SUM(c.netAmount), 0),
            COALESCE(SUM(c.doctorShare), 0),
            COALESCE(SUM(c.cosmetologistShare), 0)
          FROM CashMovement c
          WHERE c.voided = false
            AND c.type = 'IN'
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
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
      """)
  Object[] cashSplitByContextAndDateRange(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);

  // ===================================================================
  // Split de producción de la cosmetóloga (procedimiento + producto).
  // Dos ramas mutuamente excluyentes que luego se suman en el service:
  //   - FromItems: movimientos CON ítems (venta combinada y, tras Fase 2,
  //     también ventas simples y procedimientos). Split exacto por kind.
  //   - Legacy: movimientos SIN ítems (registros viejos que aún guardan el
  //     split sólo en la cabecera). Se agregan por source como antes.
  // ===================================================================

  /**
   * Produccion de la COSMETOLOGA por item: solo lo que hizo ella, con el
   * reparto de ese trabajo entre las dos.
   *
   * El filtro de autoria NO es opcional: sin el, los items propios de la
   * medica entran con doctorShare = neto completo y la card de la
   * cosmetologa termina exponiendo el dia entero de la medica.
   *
   * Va a nivel ITEM y no de movimiento porque una venta combinada puede
   * mezclar un procedimiento de Gise con una venta de Pili en el mismo
   * CashMovement: filtrar por la cabecera arrastraria los items de la medica.
   *
   * Rama hibrida, mismo criterio que el resto del sistema:
   *  - Items nuevos: performedBy dice quien lo hizo. Es el dato real.
   *  - Items viejos sin backfill posible (procedimientos con 0% para la
   *    cosmetologa): se cae al criterio anterior por monto, que es
   *    exactamente lo que ya se venia mostrando para esos registros.
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
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.context = :context
          AND (
                i.performedBy = com.jowi.stock.cash.enums.CashActor.COSMETOLOGA
             OR (i.performedBy IS NULL AND i.cosmetologistShare > 0)
              )
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
        WHERE c.voided = false
          AND c.type = 'IN'
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
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.context = :context
      """)
  Object[] salesTotalsFromItems(@Param("context") CashContext context);

  @Query("""
        SELECT
          COALESCE(SUM(CASE WHEN c.source = 'PRODUCT_SALE' THEN c.amount ELSE 0 END), 0),
          COALESCE(SUM(CASE WHEN c.source = 'PROCEDURE'    THEN c.amount ELSE 0 END), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.type = 'IN'
          AND c.context = :context
          AND c.items IS EMPTY
      """)
  Object[] salesTotalsLegacy(@Param("context") CashContext context);

  /**
   * Totales del día por método de pago y tipo (IN/OUT), para el cierre de caja.
   * Filtra anulados (voided = false), como toda agregación. Devuelve filas
   * [PaymentMethod, CashMovementType, SUM(amount)].
   */
  @Query("""
        SELECT c.paymentMethod, c.type, COALESCE(SUM(c.amount), 0)
        FROM CashMovement c
        WHERE c.voided = false
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
        GROUP BY c.paymentMethod, c.type
      """)
  List<Object[]> dailyByMethod(
      @Param("context") CashContext context,
      @Param("from") java.time.Instant from,
      @Param("to") java.time.Instant to);
}