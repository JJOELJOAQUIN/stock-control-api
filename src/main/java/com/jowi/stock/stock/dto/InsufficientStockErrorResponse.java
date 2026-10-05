package com.jowi.stock.stock.dto;

import com.jowi.stock.stock.enums.StockContext;

import java.time.Instant;
import java.util.UUID;

/**
 * Cuerpo del 409 por stock insuficiente. Mantiene los campos del
 * ErrorResponse genérico (status, error, message, path) y suma
 * {@code code = "INSUFFICIENT_STOCK"} y los datos del producto para que el
 * front pueda abrir el diálogo de reconciliación.
 */
public record InsufficientStockErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String code,
    UUID productId,
    String productName,
    StockContext context,
    int available,
    int requested,
    String unit,
    int unitsPerPackage) {

  public static final String CODE = "INSUFFICIENT_STOCK";
}
