package com.jowi.stock.dashboard;

import com.jowi.stock.movement.StockMovementRepository;
import com.jowi.stock.movement.StockMovementType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovementKpiService {

  private final StockMovementRepository repository;

  public MovementKpiService(StockMovementRepository repository) {
    this.repository = repository;
  }

  public MovementKpiResponse getKpis() {

    // Inicio del día actual (00:00) en zona local
    Instant todayStart =
        LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();

    // Últimos 7 días desde ahora
    Instant lastWeek =
        Instant.now().minusSeconds(7L * 24 * 60 * 60);

    return new MovementKpiResponse(
        repository.count(),

        repository.countByType(StockMovementType.IN),
        repository.countByType(StockMovementType.OUT),
        repository.countByType(StockMovementType.ADJUST),

        repository.countByCreatedAtAfter(todayStart),
        repository.countByCreatedAtAfter(lastWeek),

        repository.sumQuantityByType(StockMovementType.IN),
        repository.sumQuantityByType(StockMovementType.OUT)
    );
  }
}
