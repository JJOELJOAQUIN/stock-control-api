package com.jowi.stock.procedure.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Un renglón de la receta al guardarla desde el ABM: qué insumo y cuánto, en
 * la unidad consumible del producto (entero; 1,5ml = 15 si la unidad es ML).
 */
public record RecipeLineRequest(
    @NotNull UUID productId,
    @NotNull @Min(1) Integer quantity) {
}