package com.jowi.stock.purchase.exceptions;

/**
 * La orden de compra parece una que ya está cargada (mismo número de pedido,
 * o mismos productos y total hace poco). Viaja como 409 con código
 * DUPLICATE_PURCHASE: el front pregunta y, si es otra compra de verdad,
 * reenvía con confirmDuplicate = true.
 */
public class DuplicatePurchaseException extends IllegalStateException {

  public static final String CODE = "DUPLICATE_PURCHASE";

  public DuplicatePurchaseException(String message) {
    super(message);
  }
}
