package com.jowi.stock.treatment.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.treatment.entities.Treatment;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

  List<Treatment> findByContextOrderByCreatedAtDesc(CashContext context);

  List<Treatment> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

  /**
   * Tratamientos con saldo pendiente (total > suma de pagos), por contexto.
   * El cálculo se hace en la consulta para no traer todo a memoria.
   */
  @Query("""
        SELECT t FROM Treatment t
        WHERE t.context = :context
          AND t.totalAmount > (
            SELECT COALESCE(SUM(p.amount), 0)
            FROM TreatmentPayment p
            WHERE p.treatment = t
          )
        ORDER BY t.createdAt DESC
      """)
  List<Treatment> findPendingByContext(@Param("context") CashContext context);
}