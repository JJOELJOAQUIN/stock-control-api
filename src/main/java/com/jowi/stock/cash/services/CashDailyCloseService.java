package com.jowi.stock.cash.services;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.cash.dto.DailyCashSummaryResponse;
import com.jowi.stock.cash.entities.CashDailyClose;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.repositories.CashDailyCloseRepository;
import com.jowi.stock.cash.repositories.CashMovementRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Cierre de caja diario. El "preview" calcula en vivo los netos por método de
 * pago del día; el "close" persiste esa foto con quién y cuándo la cerró.
 *
 * Como toda cuenta de caja, ignora los anulados (la query ya filtra voided).
 */
@Service
@Transactional
public class CashDailyCloseService {

  private final CashMovementRepository movementRepository;
  private final CashDailyCloseRepository closeRepository;
  private final CurrentUserService currentUserService;

  public CashDailyCloseService(
      CashMovementRepository movementRepository,
      CashDailyCloseRepository closeRepository,
      CurrentUserService currentUserService) {
    this.movementRepository = movementRepository;
    this.closeRepository = closeRepository;
    this.currentUserService = currentUserService;
  }

  /** Neto por método del día. Si ya está cerrado, marca closed + quién/cuándo. */
  public DailyCashSummaryResponse preview(CashContext context, LocalDate date) {
    LocalDate day = date == null ? LocalDate.now() : date;
    Totals t = computeTotals(context, day);

    CashDailyClose existing =
        closeRepository.findByContextAndCloseDate(context, day).orElse(null);

    return new DailyCashSummaryResponse(
        context, day,
        t.net(PaymentMethod.CASH), t.net(PaymentMethod.TRANSFER),
        t.net(PaymentMethod.DEBIT), t.net(PaymentMethod.CREDIT),
        t.totalIn, t.totalOut, t.totalIn.subtract(t.totalOut),
        existing != null,
        existing == null ? null : existing.getClosedBy(),
        existing == null ? null : existing.getCreatedAt(),
        existing == null ? null : existing.getNote());
  }

  /** Persiste el cierre del día. Un día se cierra una sola vez por contexto. */
  public DailyCashSummaryResponse close(CashContext context, LocalDate date, String note) {
    LocalDate day = date == null ? LocalDate.now() : date;

    if (closeRepository.existsByContextAndCloseDate(context, day)) {
      throw new IllegalStateException("La caja de ese día ya está cerrada");
    }

    Totals t = computeTotals(context, day);

    CashDailyClose close = new CashDailyClose();
    close.setContext(context);
    close.setCloseDate(day);
    close.setCashNet(t.net(PaymentMethod.CASH));
    close.setTransferNet(t.net(PaymentMethod.TRANSFER));
    close.setDebitNet(t.net(PaymentMethod.DEBIT));
    close.setCreditNet(t.net(PaymentMethod.CREDIT));
    close.setTotalIn(t.totalIn);
    close.setTotalOut(t.totalOut);
    close.setNetTotal(t.totalIn.subtract(t.totalOut));
    close.setClosedBy(currentUserService.currentUserLabel());
    close.setNote(note == null || note.isBlank() ? null : note.trim());

    CashDailyClose saved = closeRepository.save(close);

    return new DailyCashSummaryResponse(
        context, day,
        saved.getCashNet(), saved.getTransferNet(),
        saved.getDebitNet(), saved.getCreditNet(),
        saved.getTotalIn(), saved.getTotalOut(), saved.getNetTotal(),
        true, saved.getClosedBy(), saved.getCreatedAt(), saved.getNote());
  }

  public List<DailyCashSummaryResponse> history(CashContext context) {
    return closeRepository.findByContextOrderByCloseDateDesc(context).stream()
        .map(c -> new DailyCashSummaryResponse(
            c.getContext(), c.getCloseDate(),
            c.getCashNet(), c.getTransferNet(), c.getDebitNet(), c.getCreditNet(),
            c.getTotalIn(), c.getTotalOut(), c.getNetTotal(),
            true, c.getClosedBy(), c.getCreatedAt(), c.getNote()))
        .toList();
  }

  // ── Cálculo ──

  private Totals computeTotals(CashContext context, LocalDate day) {
    ZoneId zone = ZoneId.systemDefault();
    Instant from = day.atStartOfDay(zone).toInstant();
    Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();

    Totals t = new Totals();
    for (Object[] row : movementRepository.dailyByMethod(context, from, to)) {
      PaymentMethod method = (PaymentMethod) row[0];
      CashMovementType type = (CashMovementType) row[1];
      BigDecimal amount = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
      if (method == null) {
        continue;
      }
      if (type == CashMovementType.IN) {
        t.in.merge(method, amount, BigDecimal::add);
        t.totalIn = t.totalIn.add(amount);
      } else {
        t.out.merge(method, amount, BigDecimal::add);
        t.totalOut = t.totalOut.add(amount);
      }
    }
    return t;
  }

  private static final class Totals {
    final Map<PaymentMethod, BigDecimal> in = new EnumMap<>(PaymentMethod.class);
    final Map<PaymentMethod, BigDecimal> out = new EnumMap<>(PaymentMethod.class);
    BigDecimal totalIn = BigDecimal.ZERO;
    BigDecimal totalOut = BigDecimal.ZERO;

    BigDecimal net(PaymentMethod m) {
      return in.getOrDefault(m, BigDecimal.ZERO)
          .subtract(out.getOrDefault(m, BigDecimal.ZERO));
    }
  }
}