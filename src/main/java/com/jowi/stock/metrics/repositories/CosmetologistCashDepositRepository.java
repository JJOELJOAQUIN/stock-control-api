package com.jowi.stock.metrics.repositories;

import com.jowi.stock.metrics.entities.CosmetologistCashDeposit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CosmetologistCashDepositRepository
    extends JpaRepository<CosmetologistCashDeposit, UUID> {

  /** Entregas de un rango de días [from, to], de la más vieja a la más nueva. */
  List<CosmetologistCashDeposit> findByDepositDateBetweenOrderByDepositDateAscCreatedAtAsc(
      LocalDate from, LocalDate to);
}
