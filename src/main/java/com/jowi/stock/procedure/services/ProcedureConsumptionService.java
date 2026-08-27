package com.jowi.stock.procedure.services;

import com.jowi.stock.procedure.entities.ProcedureConsumption;
import com.jowi.stock.procedure.repositories.ProcedureConsumptionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

/**
 * Resuelve la receta (BOM) de un procedimiento: dado un procedureCode,
 * devuelve qué insumos consume y cuánto. Es la fuente de verdad server-side
 * del consumo, análoga al resolveProcedureSplit del reparto: el cliente ya no
 * decide qué se descuenta, lo decide la receta.
 */
@Service
public class ProcedureConsumptionService {

  private final ProcedureConsumptionRepository repository;

  public ProcedureConsumptionService(ProcedureConsumptionRepository repository) {
    this.repository = repository;
  }

  /** Una línea de consumo resuelta: producto + cantidad en su unidad. */
  public record ResolvedLine(UUID productId, int quantity) {}

  /** true si el procedimiento tiene receta cargada. */
  public boolean hasRecipe(String procedureCode) {
    if (procedureCode == null || procedureCode.isBlank()) return false;
    return repository.existsByProcedureCode(procedureCode.trim());
  }

  /**
   * Receta del procedimiento como líneas de consumo. Vacía si no hay receta
   * (el llamador decide el fallback).
   */
  public List<ResolvedLine> resolve(String procedureCode) {
    if (procedureCode == null || procedureCode.isBlank()) return List.of();
    return repository.findByProcedureCode(procedureCode.trim()).stream()
        .map(this::toLine)
        .toList();
  }

  private ResolvedLine toLine(ProcedureConsumption c) {
    return new ResolvedLine(c.getProductId(), c.getQuantity());
  }
}
