package com.jowi.stock.procedure.services;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.procedure.dto.ProcedureCatalogRequest;
import com.jowi.stock.procedure.dto.ProcedureCatalogResponse;
import com.jowi.stock.procedure.entities.ProcedureCatalog;
import com.jowi.stock.procedure.enums.ProcedureKind;
import com.jowi.stock.procedure.enums.ProcedureSplitRule;
import com.jowi.stock.procedure.repositories.ProcedureCatalogRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * ABM del catálogo de tratamientos. Toda la traducción "regla de reparto →
 * (kind, performer, %médica, %cosmetóloga)" vive acá y en un solo lugar: es
 * lo que garantiza que no se pueda cargar un reparto inválido.
 */
@Service
@Transactional
public class ProcedureCatalogService {

  private static final BigDecimal ONE = BigDecimal.ONE;
  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal P70 = new BigDecimal("0.70");
  private static final BigDecimal P30 = new BigDecimal("0.30");
  private static final BigDecimal HALF = new BigDecimal("0.50");

  private final ProcedureCatalogRepository repository;

  public ProcedureCatalogService(ProcedureCatalogRepository repository) {
    this.repository = repository;
  }

  public List<ProcedureCatalogResponse> list(boolean includeInactive) {
    List<ProcedureCatalog> rows = includeInactive
        ? repository.findAllByOrderByKindAscLabelAsc()
        : repository.findByActiveTrueOrderByKindAscLabelAsc();
    return rows.stream().map(c -> ProcedureCatalogResponse.from(c, ruleOf(c))).toList();
  }

  public ProcedureCatalogResponse create(ProcedureCatalogRequest req) {
    String code = normalizeCode(req.code());
    if (repository.existsByCodeIgnoreCase(code)) {
      throw new IllegalArgumentException(
          "Ya existe un tratamiento con el código " + code);
    }
    ProcedureCatalog c = new ProcedureCatalog();
    c.setCode(code);
    apply(c, req);
    ProcedureCatalog saved = repository.save(c);
    return ProcedureCatalogResponse.from(saved, req.splitRule());
  }

  public ProcedureCatalogResponse update(UUID id, ProcedureCatalogRequest req) {
    ProcedureCatalog c = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Tratamiento no encontrado"));

    String code = normalizeCode(req.code());
    // Si cambia el código, no puede pisar a otro existente.
    if (!code.equalsIgnoreCase(c.getCode())
        && repository.existsByCodeIgnoreCase(code)) {
      throw new IllegalArgumentException(
          "Ya existe otro tratamiento con el código " + code);
    }
    c.setCode(code);
    apply(c, req);
    ProcedureCatalog saved = repository.save(c);
    return ProcedureCatalogResponse.from(saved, req.splitRule());
  }

  /** Baja lógica: nunca borramos, así el histórico y las métricas no se rompen. */
  public ProcedureCatalogResponse setActive(UUID id, boolean active) {
    ProcedureCatalog c = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Tratamiento no encontrado"));
    c.setActive(active);
    ProcedureCatalog saved = repository.save(c);
    return ProcedureCatalogResponse.from(saved, ruleOf(saved));
  }

  // ── Mapeo regla → campos (la única autoridad del reparto) ──

  private void apply(ProcedureCatalog c, ProcedureCatalogRequest req) {
    c.setLabel(req.label().trim());
    c.setAmount(req.amount());
    switch (req.splitRule()) {
      case MEDICA_100 -> {
        c.setKind(ProcedureKind.MEDICA);
        c.setPerformer(CashActor.MEDICA);
        c.setDoctorPercent(ONE);
        c.setCosmetologistPercent(ZERO);
      }
      case COSMO_70_30 -> {
        c.setKind(ProcedureKind.COSMETOLOGIA);
        c.setPerformer(CashActor.COSMETOLOGA);
        c.setDoctorPercent(P30);
        c.setCosmetologistPercent(P70);
      }
      case COSMO_50_50 -> {
        c.setKind(ProcedureKind.COSMETOLOGIA);
        c.setPerformer(CashActor.COSMETOLOGA);
        c.setDoctorPercent(HALF);
        c.setCosmetologistPercent(HALF);
      }
    }
  }

  /** Reconstruye la regla desde los campos guardados (para devolverla al form). */
  private ProcedureSplitRule ruleOf(ProcedureCatalog c) {
    if (c.getPerformer() == CashActor.MEDICA) {
      return ProcedureSplitRule.MEDICA_100;
    }
    if (c.getCosmetologistPercent() != null
        && c.getCosmetologistPercent().compareTo(HALF) == 0) {
      return ProcedureSplitRule.COSMO_50_50;
    }
    return ProcedureSplitRule.COSMO_70_30;
  }

  private String normalizeCode(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
      throw new IllegalArgumentException("El código es obligatorio");
    }
    return raw.trim().toUpperCase().replace(' ', '_');
  }
}