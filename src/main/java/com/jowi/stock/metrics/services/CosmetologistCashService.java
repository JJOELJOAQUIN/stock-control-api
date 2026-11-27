package com.jowi.stock.metrics.services;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.common.BusinessZone;
import com.jowi.stock.metrics.dto.CosmetologistCashResponse;
import com.jowi.stock.metrics.dto.CreateCosmetologistCashRequest;
import com.jowi.stock.metrics.entities.CosmetologistCashDeposit;
import com.jowi.stock.metrics.repositories.CosmetologistCashDepositRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro del efectivo que la cosmetóloga le deja a la médica. Lo cargan la
 * cosmetóloga (cada vez que deja plata, incluso si deja de más) y la médica.
 * Cada una puede borrar lo que cargó; la médica, cualquier entrega.
 */
@Service
@Transactional
public class CosmetologistCashService {

  private final CosmetologistCashDepositRepository repository;
  private final CurrentUserService currentUserService;

  public CosmetologistCashService(
      CosmetologistCashDepositRepository repository,
      CurrentUserService currentUserService) {
    this.repository = repository;
    this.currentUserService = currentUserService;
  }

  @Transactional(readOnly = true)
  public CosmetologistCashResponse monthly(int year, int month) {
    if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1..12");

    YearMonth ym = YearMonth.of(year, month);
    List<CosmetologistCashDeposit> deposits =
        repository.findByDepositDateBetweenOrderByDepositDateAscCreatedAtAsc(
            ym.atDay(1), ym.atEndOfMonth());

    BigDecimal total = deposits.stream()
        .map(CosmetologistCashDeposit::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CosmetologistCashResponse(
        year, month, total,
        deposits.stream().map(CosmetologistCashResponse.Entry::from).toList());
  }

  public CosmetologistCashResponse.Entry create(CreateCosmetologistCashRequest req) {
    if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El monto debe ser mayor a cero");
    }

    LocalDate date = req.date() != null ? req.date() : BusinessZone.today();
    if (date.isAfter(BusinessZone.today())) {
      throw new IllegalArgumentException("La fecha no puede ser futura");
    }

    CosmetologistCashDeposit d = new CosmetologistCashDeposit();
    d.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
    d.setDepositDate(date);
    d.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
    d.setCreatedBy(currentUserService.currentUserLabel());

    return CosmetologistCashResponse.Entry.from(repository.save(d));
  }

  public void delete(UUID id) {
    CosmetologistCashDeposit d = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada: " + id));

    if (!currentUserService.isAdmin()
        && !d.getCreatedBy().equalsIgnoreCase(currentUserService.currentUserLabel())) {
      throw new IllegalStateException("Solo podés borrar el efectivo que cargaste vos");
    }

    repository.delete(d);
  }
}
