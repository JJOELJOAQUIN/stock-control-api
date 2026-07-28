package com.jowi.stock.metrics.services;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.metrics.dto.MonthlyMetricsResponse;
import com.jowi.stock.metrics.repositories.MetricsRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

  private final MetricsRepository repository;
  private final CurrentUserService currentUserService;

  public MetricsService(
      MetricsRepository repository,
      CurrentUserService currentUserService) {
    this.repository = repository;
    this.currentUserService = currentUserService;
  }

  public MonthlyMetricsResponse monthly(CashContext context, int year, int month) {
    if (context == null) throw new IllegalArgumentException("context is required");
    if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1..12");

    ZoneId zone = ZoneId.systemDefault();
    LocalDate first = LocalDate.of(year, month, 1);
    Instant from = first.atStartOfDay(zone).toInstant();
    Instant to = first.plusMonths(1).atStartOfDay(zone).toInstant();

    // Un procedimiento puede venir por su flujo propio o dentro de una venta
    // combinada. Se acumulan por código para que la métrica no dependa de
    // por dónde entró la plata.
    Map<String, long[]> counts = new LinkedHashMap<>();
    Map<String, BigDecimal[]> sums = new LinkedHashMap<>();

    for (Object[] row : repository.proceduresByItems(context, from, to)) {
      accumulate(counts, sums, row);
    }

    List<MonthlyMetricsResponse.ProcedureMetricRow> procedures = new ArrayList<>();
    for (Map.Entry<String, long[]> e : counts.entrySet()) {
      BigDecimal[] s = sums.get(e.getKey());
      procedures.add(new MonthlyMetricsResponse.ProcedureMetricRow(
          e.getKey(), e.getValue()[0], s[0], s[1], s[2], s[3]));
    }

    // De mayor a menor: primero el procedimiento que más se hizo, y a igual
    // cantidad el que más facturó. Es el orden que pidió la Dra para la card.
    procedures.sort(
        java.util.Comparator
            .comparingLong(MonthlyMetricsResponse.ProcedureMetricRow::count)
            .thenComparing(MonthlyMetricsResponse.ProcedureMetricRow::amount)
            .reversed());

    Object[] ph = first(repository.productsFromHeader(context, from, to));
    Object[] pi = first(repository.productsFromItems(context, from, to));

    MonthlyMetricsResponse.ProductMetricRow products =
        new MonthlyMetricsResponse.ProductMetricRow(
            num(ph, 0).longValue() + num(pi, 0).longValue(),
            num(ph, 1).add(num(pi, 1)),
            num(ph, 2).add(num(pi, 2)),
            num(ph, 3).add(num(pi, 3)),
            num(ph, 4).add(num(pi, 4)));

    // Blindaje por rol. La cosmetóloga recibe SOLO lo suyo: los
    // procedimientos donde le tocó algo, y de esas filas únicamente su
    // parte. El bruto y la parte de la médica se ponen en cero antes de
    // salir del servidor — si viajaran, la parte de la médica se deduce
    // restando, y "no mostrar" en el front no es lo mismo que "no enviar".
    if (currentUserService.isCosmetologist()) {
      procedures = procedures.stream()
          .filter(p -> p.cosmetologistShare().compareTo(BigDecimal.ZERO) > 0)
          .map(p -> new MonthlyMetricsResponse.ProcedureMetricRow(
              p.procedureCode(),
              p.count(),
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              p.cosmetologistShare()))
          .toList();

      products = new MonthlyMetricsResponse.ProductMetricRow(
          products.count(),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          products.cosmetologistShare());
    }

    return new MonthlyMetricsResponse(year, month, context, procedures, products);
  }

  private void accumulate(
      Map<String, long[]> counts, Map<String, BigDecimal[]> sums, Object[] row) {

    String code = (String) row[0];
    long qty = ((Number) row[1]).longValue();

    counts.computeIfAbsent(code, k -> new long[] { 0 })[0] += qty;

    BigDecimal[] s = sums.computeIfAbsent(code, k -> new BigDecimal[] {
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });

    s[0] = s[0].add(dec(row[2]));
    s[1] = s[1].add(dec(row[3]));
    s[2] = s[2].add(dec(row[4]));
    s[3] = s[3].add(dec(row[5]));
  }

  private Object[] first(List<Object[]> rows) {
    return rows.isEmpty() ? null : rows.get(0);
  }

  private BigDecimal num(Object[] row, int i) {
    return row == null ? BigDecimal.ZERO : dec(row[i]);
  }

  private BigDecimal dec(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal b) return b;
    return BigDecimal.valueOf(((Number) value).doubleValue());
  }
}