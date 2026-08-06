package com.jowi.stock.toxina.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.services.StockService;
import com.jowi.stock.toxina.entities.OpenVial;
import com.jowi.stock.toxina.entities.ToxinaSession;
import com.jowi.stock.toxina.enums.OpenVialStatus;
import com.jowi.stock.toxina.repositories.OpenVialRepository;
import com.jowi.stock.toxina.repositories.ToxinaSessionRepository;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.services.TreatmentService;

import jakarta.transaction.Transactional;

/**
 * Toxina (Xeomin). El tratamiento y el pago se delegan a {@link TreatmentService}
 * (code = TOXINA_XEOMIN, pago único, 100% médica). Lo propio de acá es el vial
 * abierto compartido y las sesiones con unidades cargadas a mano.
 */
@Service
@Transactional
public class ToxinaService {

  /** Code del tratamiento genérico para toxina. */
  public static final String CODE = "TOXINA_XEOMIN";

  /** Un vial de Xeomin = 2 ml × 50 U/ml. */
  private static final int UNITS_PER_VIAL = 100;

  /** Un vial abierto se puede usar durante 20 días. */
  private static final int SHELF_LIFE_DAYS = 20;

  /** Pago único. */
  private static final int MAX_INSTALLMENTS = 1;

  private final TreatmentService treatmentService;
  private final StockService stockService;
  private final ProductService productService;
  private final OpenVialRepository openVialRepository;
  private final ToxinaSessionRepository toxinaSessionRepository;

  public ToxinaService(
      TreatmentService treatmentService,
      StockService stockService,
      ProductService productService,
      OpenVialRepository openVialRepository,
      ToxinaSessionRepository toxinaSessionRepository) {
    this.treatmentService = treatmentService;
    this.stockService = stockService;
    this.productService = productService;
    this.openVialRepository = openVialRepository;
    this.toxinaSessionRepository = toxinaSessionRepository;
  }

  // ======================= TRATAMIENTO =======================

  /**
   * Crea el tratamiento de toxina para un paciente. Reusa el flujo genérico:
   * pago único (maxInstallments = 1), sin fijo de cosmetóloga (100% médica).
   * El pago se registra después por POST /api/treatments/{id}/payments.
   */
  public Treatment createTreatment(UUID patientId, BigDecimal totalAmount, String description) {
    String desc = (description == null || description.isBlank())
        ? "Toxina botulínica (Xeomin)"
        : description.trim();
    return treatmentService.createTreatment(patientId, CODE, desc, totalAmount, null, MAX_INSTALLMENTS);
  }

  // ======================= SESIONES =======================

  /**
   * Registra una sesión: resuelve un vial abierto (reusa el más viejo con
   * unidades o abre uno nuevo descontando 1 del stock), le descuenta las
   * unidades y guarda la sesión atada al tratamiento.
   */
  public ToxinaSession registerSession(
      UUID treatmentId,
      UUID productId,
      int sessionNumber,
      int unitsUsed,
      CashContext context) {

    if (unitsUsed <= 0)
      throw new IllegalArgumentException("Las unidades aplicadas deben ser mayor a cero");
    if (unitsUsed > UNITS_PER_VIAL)
      throw new IllegalArgumentException(
          "Las unidades de una sesión no pueden superar las de un vial (" + UNITS_PER_VIAL + ")");
    if (sessionNumber < 1)
      throw new IllegalArgumentException("El número de sesión debe ser al menos 1");

    Treatment treatment = treatmentService.getTreatment(treatmentId);
    CashContext ctx = context == null ? CashContext.CONSULTORIO : context;

    OpenVial vial = findOrOpenVial(productId, unitsUsed, ctx);

    int remaining = vial.getUnitsRemaining() - unitsUsed;
    vial.setUnitsRemaining(Math.max(remaining, 0));
    if (vial.getUnitsRemaining() == 0) {
      vial.setStatus(OpenVialStatus.DEPLETED);
    }
    openVialRepository.save(vial);

    ToxinaSession session = new ToxinaSession();
    session.setTreatment(treatment);
    session.setOpenVial(vial);
    session.setSessionNumber(sessionNumber);
    session.setPerformedAt(Instant.now());
    session.setUnitsUsed(unitsUsed);
    return toxinaSessionRepository.save(session);
  }

  public List<ToxinaSession> getSessions(UUID treatmentId) {
    return toxinaSessionRepository.findByTreatmentIdOrderBySessionNumberAsc(treatmentId);
  }

  // ======================= VIAL =======================

  /**
   * Devuelve un vial abierto usable para el producto o abre uno nuevo.
   * Reusa el más viejo (FIFO) para gastar antes el que está por vencer.
   * De paso marca EXPIRED los que pasaron los 20 días.
   */
  private OpenVial findOrOpenVial(UUID productId, int unitsNeeded, CashContext context) {
    Instant now = Instant.now();

    OpenVial chosen = null;
    for (OpenVial v : openVialRepository
        .findByProductIdAndStatusOrderByOpenedAtAsc(productId, OpenVialStatus.OPEN)) {

      if (!v.getExpiresAt().isAfter(now)) {   // venció
        v.setStatus(OpenVialStatus.EXPIRED);
        openVialRepository.save(v);
        continue;
      }
      if (chosen == null && v.getUnitsRemaining() >= unitsNeeded) {
        chosen = v;                            // el más viejo con unidades
      }
    }
    if (chosen != null)
      return chosen;

    // No hay vial usable: abrir uno nuevo. decrease tira si no hay stock y
    // revierte toda la transacción (no se registra la sesión sin vial).
    stockService.decrease(productId, context.toStockContext(), 1,
        StockMovementReason.PROCEDIMIENTO, "Apertura de vial de toxina");

    Product product = productService.getById(productId);

    OpenVial vial = new OpenVial();
    vial.setProduct(product);
    vial.setOpenedAt(now);
    vial.setExpiresAt(now.plus(SHELF_LIFE_DAYS, ChronoUnit.DAYS));
    vial.setTotalUnits(UNITS_PER_VIAL);
    vial.setUnitsRemaining(UNITS_PER_VIAL);
    vial.setStatus(OpenVialStatus.OPEN);
    return openVialRepository.save(vial);
  }
}