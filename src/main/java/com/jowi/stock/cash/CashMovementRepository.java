package com.jowi.stock.cash;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
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

}
