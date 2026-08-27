package com.jowi.stock.alert;

import com.jowi.stock.product.entities.Product;
import com.jowi.stock.stock.entities.StockEntity;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.repositories.JpaStockRepository;
import com.jowi.stock.toxina.entities.OpenVial;
import com.jowi.stock.toxina.enums.OpenVialStatus;
import com.jowi.stock.toxina.repositories.OpenVialRepository;
import com.jowi.stock.toxina.repositories.ToxinaSessionRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertService {

  /** Días desde que se abre un vial hasta que se avisa que está por vencer. */
  private static final int VIAL_ALERT_AFTER_DAYS = 10;

  private static final DateTimeFormatter DAY_FMT =
      DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

  private final JpaStockRepository stockRepository;
  private final OpenVialRepository openVialRepository;
  private final ToxinaSessionRepository toxinaSessionRepository;

  public AlertService(
      JpaStockRepository stockRepository,
      OpenVialRepository openVialRepository,
      ToxinaSessionRepository toxinaSessionRepository) {
    this.stockRepository = stockRepository;
    this.openVialRepository = openVialRepository;
    this.toxinaSessionRepository = toxinaSessionRepository;
  }

  // =========================
  // ALERTAS STOCK BAJO
  // =========================
  public List<AlertResponse> lowStock(StockContext context) {
    return stockRepository.findByContext(context)
        .stream()
        .filter(stock -> stock.getCurrent() < stock.getProduct().getMinimumStock())
        .map(this::toLowStockAlert)
        .toList();
  }

  // =========================
  // ALERTAS SIN STOCK
  // =========================
  public List<AlertResponse> outOfStock(StockContext context) {
    return stockRepository.findByContext(context)
        .stream()
        .filter(stock -> stock.getCurrent() == 0)
        .map(this::toOutOfStockAlert)
        .toList();
  }

  // =========================
  // VIALES ABIERTOS POR VENCER (toxina)
  // =========================
  /**
   * Avisa por cada vial abierto que ya pasó los {@value #VIAL_ALERT_AFTER_DAYS}
   * días (día 10 de 20): dice cuándo vence, unidades restantes y los pacientes
   * asociados, para recordar la 2da sesión. De paso marca EXPIRED los vencidos.
   * Es método de escritura (override del readOnly de la clase) por ese marcado.
   */
  @Transactional
  public List<AlertResponse> openVialsExpiring() {
    Instant now = Instant.now();
    List<AlertResponse> alerts = new ArrayList<>();

    for (OpenVial vial : openVialRepository.findByStatusOrderByOpenedAtAsc(OpenVialStatus.OPEN)) {

      if (!vial.getExpiresAt().isAfter(now)) {
        vial.setStatus(OpenVialStatus.EXPIRED);
        openVialRepository.save(vial);
        continue;
      }

      Instant alertFrom = vial.getOpenedAt().plus(VIAL_ALERT_AFTER_DAYS, ChronoUnit.DAYS);
      if (now.isBefore(alertFrom)) {
        continue;
      }

      String pacientes = toxinaSessionRepository.findByOpenVialId(vial.getId()).stream()
          .map(s -> s.getTreatment().getPatient())
          .map(p -> p.getFirstName() + " " + p.getLastName())
          .distinct()
          .collect(Collectors.joining(", "));

      StringBuilder msg = new StringBuilder("Vial abierto el ")
          .append(DAY_FMT.format(vial.getOpenedAt()))
          .append(", vence el ").append(DAY_FMT.format(vial.getExpiresAt()))
          .append(". Unidades restantes: ").append(vial.getUnitsRemaining());
      if (!pacientes.isBlank()) {
        msg.append(". Pacientes: ").append(pacientes).append(" — agendá la 2da sesión");
      }

      Product product = vial.getProduct();
      alerts.add(new AlertResponse(
          AlertType.EXPIRING_SOON,
          product.getId(),
          product.getName(),
          msg.toString(),
          toOffset(vial.getOpenedAt())));
    }

    return alerts;
  }

  // =========================
  // HELPERS
  // =========================
  private AlertResponse toLowStockAlert(StockEntity stock) {
    Product product = stock.getProduct();

    return new AlertResponse(
        AlertType.LOW_STOCK,
        product.getId(),
        product.getName(),
        "Stock por debajo del mínimo (" + stock.getCurrent() + "/" + product.getMinimumStock() + ")",
        OffsetDateTime.now());
  }

  private AlertResponse toOutOfStockAlert(StockEntity stock) {
    Product product = stock.getProduct();

    return new AlertResponse(
        AlertType.OUT_OF_STOCK,
        product.getId(),
        product.getName(),
        "Producto sin stock",
        OffsetDateTime.now());
  }

  private OffsetDateTime toOffset(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
