package com.jowi.stock.procedure.dto;

/**
 * Un renglón de la receta para mostrar en el ABM: además del producto y la
 * cantidad, trae el nombre y la unidad (ML / AMPOLLA / DISPARO / UNIDAD) para
 * que se vea "2 · AMPOLLA · MESOHYAL DMAE".
 */
public record RecipeLineResponse(
    String productId,
    String productName,
    String unit,
    int quantity) {
}