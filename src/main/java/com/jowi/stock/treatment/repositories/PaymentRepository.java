package com.jowi.stock.treatment.repositories;

import com.jowi.stock.treatment.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
  List<Payment> findByTreatmentIdOrderByInstallmentNumberAsc(UUID treatmentId);
   Optional<Payment> findByCashMovementId(UUID cashMovementId);
}

