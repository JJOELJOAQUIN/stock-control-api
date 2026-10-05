package com.jowi.stock.stock.exceptions;

import com.jowi.stock.stock.enums.StockContext;

import java.util.UUID;

/**
 * El stock de un producto no alcanza para la salida pedida.
 *
 * Extiende IllegalStateException para no romper a quien ya la atrapaba como
 * tal, pero viaja con los datos del producto: el front los usa para ofrecer
 * "Editar stock" (reconciliar con el conteo real) y reintentar la carga, en
 * vez de dejar a la persona trabada con un "Insufficient stock" genérico.
 */
public class InsufficientStockException extends IllegalStateException {

  private final UUID productId;
  private final String productName;
  private final StockContext context;
  private final int available;
  private final int requested;
  private final String unit;
  private final int unitsPerPackage;

  public InsufficientStockException(
      UUID productId,
      String productName,
      StockContext context,
      int available,
      int requested,
      String unit,
      int unitsPerPackage) {
    super("Stock insuficiente de " + productName + ": hay " + available
        + " " + unit + " y se necesitan " + requested + ".");
    this.productId = productId;
    this.productName = productName;
    this.context = context;
    this.available = available;
    this.requested = requested;
    this.unit = unit;
    this.unitsPerPackage = unitsPerPackage;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public StockContext getContext() {
    return context;
  }

  public int getAvailable() {
    return available;
  }

  public int getRequested() {
    return requested;
  }

  public String getUnit() {
    return unit;
  }

  public int getUnitsPerPackage() {
    return unitsPerPackage;
  }
}
