package com.jowi.stock.procedure.dto;

import com.jowi.stock.procedure.entities.ProcedureCatalog;
import com.jowi.stock.procedure.enums.ProcedureKind;
import com.jowi.stock.procedure.enums.ProcedureSpecialFlow;
import com.jowi.stock.procedure.enums.ProcedureSplitRule;
import java.math.BigDecimal;

/**
 * Lo que ve el front de un tratamiento. Incluye tanto la regla (para el form
 * del ABM) como los campos derivados (para mostrar el reparto ya resuelto).
 */
public record ProcedureCatalogResponse(
    String id,
    String code,
    String label,
    ProcedureKind kind,
    String performer,
    BigDecimal doctorPercent,
    BigDecimal cosmetologistPercent,
    BigDecimal amount,
    boolean active,
    ProcedureSplitRule splitRule,
    ProcedureSpecialFlow specialFlow) {

  public static ProcedureCatalogResponse from(ProcedureCatalog c, ProcedureSplitRule rule) {
    return new ProcedureCatalogResponse(
        c.getId() == null ? null : c.getId().toString(),
        c.getCode(),
        c.getLabel(),
        c.getKind(),
        c.getPerformer() == null ? null : c.getPerformer().name(),
        c.getDoctorPercent(),
        c.getCosmetologistPercent(),
        c.getAmount(),
        c.isActive(),
        rule,
        c.getSpecialFlow());
  }
}
