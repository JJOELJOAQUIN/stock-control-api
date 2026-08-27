package com.jowi.stock.movement.enums;

public enum StockMovementReason {
  COMPRA_PROVEEDOR,
  VENTA,
  USO_CAMILLA,
  AJUSTE_ERROR,
  // Consumo de insumos al registrar un tratamiento dermatológico (BOM).
  PROCEDIMIENTO,
  // Reingreso de stock al anular una venta. El código que lo emite llega con
  // el feature de anulación; el valor va ahora para no tocar el enum (ni la
  // constraint de Neon) dos veces.
  ANULACION,
  VENCIMIENTO,
  TRASLADO,
  USO_PERSONAL,
  MUESTRA,
  REGALO,
  PEDIDO_ESPECIAL,
  OTRO
}
