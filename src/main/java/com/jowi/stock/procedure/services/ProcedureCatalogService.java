package com.jowi.stock.procedure.services;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.procedure.dto.ProcedureCatalogRequest;
import com.jowi.stock.procedure.dto.ProcedureCatalogResponse;
import com.jowi.stock.procedure.dto.RecipeLineRequest;
import com.jowi.stock.procedure.dto.RecipeLineResponse;
import com.jowi.stock.procedure.entities.ProcedureCatalog;
import com.jowi.stock.procedure.entities.ProcedureConsumption;
import com.jowi.stock.procedure.enums.ProcedureKind;
import com.jowi.stock.procedure.enums.ProcedureSpecialFlow;
import com.jowi.stock.procedure.enums.ProcedureSplitRule;
import com.jowi.stock.procedure.repositories.ProcedureCatalogRepository;
import com.jowi.stock.procedure.repositories.ProcedureConsumptionRepository;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final ProcedureConsumptionRepository consumptionRepository;
  private final ProductRepository productRepository;

  public ProcedureCatalogService(
      ProcedureCatalogRepository repository,
      ProcedureConsumptionRepository consumptionRepository,
      ProductRepository productRepository) {
    this.repository = repository;
    this.consumptionRepository = consumptionRepository;
    this.productRepository = productRepository;
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

  // ── Receta (BOM) por tratamiento ──
  // Es lo que consume ProcedureConsumptionService al pasar la sesión (suelto o
  // combinada). Editarla acá cambia el consumo sin tocar código.

  public List<RecipeLineResponse> getRecipe(UUID id) {
    ProcedureCatalog c = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Tratamiento no encontrado"));
    return recipeOf(c.getCode());
  }

  /** Reemplaza TODA la receta del tratamiento por las líneas dadas. */
  public List<RecipeLineResponse> setRecipe(UUID id, List<RecipeLineRequest> lines) {
    ProcedureCatalog c = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Tratamiento no encontrado"));

    List<RecipeLineRequest> clean = lines == null ? List.of() : lines;

    // Sin insumos repetidos: la unidad de la receta es (código, producto).
    Set<UUID> seen = new HashSet<>();
    for (RecipeLineRequest l : clean) {
      if (l.productId() == null) {
        throw new IllegalArgumentException("Falta el producto en un renglón de la receta");
      }
      if (l.quantity() == null || l.quantity() < 1) {
        throw new IllegalArgumentException("La cantidad debe ser mayor o igual a 1");
      }
      if (!seen.add(l.productId())) {
        throw new IllegalArgumentException("Hay un insumo repetido en la receta");
      }
    }

    // Reemplazo total: borro lo que había y cargo lo nuevo.
    List<ProcedureConsumption> existing =
        consumptionRepository.findByProcedureCode(c.getCode());
    if (!existing.isEmpty()) {
      consumptionRepository.deleteAll(existing);
    }
    for (RecipeLineRequest l : clean) {
      ProcedureConsumption pc = new ProcedureConsumption();
      pc.setProcedureCode(c.getCode());
      pc.setProductId(l.productId());
      pc.setQuantity(l.quantity());
      consumptionRepository.save(pc);
    }
    return recipeOf(c.getCode());
  }

  private List<RecipeLineResponse> recipeOf(String code) {
    List<ProcedureConsumption> lines = consumptionRepository.findByProcedureCode(code);
    if (lines.isEmpty()) {
      return List.of();
    }
    Map<UUID, Product> byId = productRepository
        .findAllById(lines.stream().map(ProcedureConsumption::getProductId).toList())
        .stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));

    return lines.stream()
        .map(l -> {
          Product p = byId.get(l.getProductId());
          String name = p != null ? p.getName() : "(producto no encontrado)";
          String unit = p != null && p.getConsumptionUnit() != null
              ? p.getConsumptionUnit().name()
              : null;
          return new RecipeLineResponse(
              l.getProductId().toString(), name, unit, l.getQuantity());
        })
        .toList();
  }

  // ── Mapeo regla → campos (la única autoridad del reparto) ──

  private void apply(ProcedureCatalog c, ProcedureCatalogRequest req) {
    c.setLabel(req.label().trim());
    c.setAmount(req.amount());
    c.setSpecialFlow(
        req.specialFlow() == null ? ProcedureSpecialFlow.NONE : req.specialFlow());
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