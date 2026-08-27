package com.jowi.stock.cash.repositories;

import com.jowi.stock.cash.entities.CashDailyClose;
import com.jowi.stock.cash.enums.CashContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashDailyCloseRepository extends JpaRepository<CashDailyClose, UUID> {

  Optional<CashDailyClose> findByContextAndCloseDate(CashContext context, LocalDate closeDate);

  boolean existsByContextAndCloseDate(CashContext context, LocalDate closeDate);

  List<CashDailyClose> findByContextOrderByCloseDateDesc(CashContext context);
}
