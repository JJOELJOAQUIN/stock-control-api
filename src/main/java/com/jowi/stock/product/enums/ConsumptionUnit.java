package com.jowi.stock.product.enums;

/**
 * Unidad en la que se CUENTA el stock de un producto. Para el retail es
 * UNIDAD (como siempre). Para los insumos de procedimientos es la unidad
 * consumible: ML (viales que se fraccionan por sesión, ej. NCTF), AMPOLLA
 * (viales de un solo uso, ej. DMAE) o DISPARO (pines de Frax Face).
 *
 * Con la unidad bien elegida, todo consumo del manual es un entero y el
 * motor de stock/lotes no cambia: la compra entra en envases y multiplica
 * por unitsPerPackage al ingresar.
 */
public enum ConsumptionUnit {
  UNIDAD,
  ML,
  AMPOLLA,
  DISPARO
}