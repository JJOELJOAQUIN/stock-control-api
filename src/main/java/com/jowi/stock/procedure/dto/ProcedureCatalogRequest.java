package com.jowi.stock.procedure.dto;

import com.jowi.stock.procedure.enums.ProcedureSplitRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Alta / edición de un tratamiento del catálogo. El reparto viaja como una
 * sola regla (splitRule): de ahí el service deriva kind, performer y los dos
 * porcentajes. No se aceptan porcentajes sueltos a propósito.
 *
 * amount es opcional: null o 0 = "a convenir".
 */
public record ProcedureCatalogRequest(
    @NotBlank String code,
    @NotBlank String label,
    @NotNull ProcedureSplitRule splitRule,
    BigDecimal amount) {
}