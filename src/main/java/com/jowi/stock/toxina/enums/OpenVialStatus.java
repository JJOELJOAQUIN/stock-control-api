package com.jowi.stock.toxina.enums;

/**
 * Estado de un vial abierto de toxina.
 * - OPEN:     abierto y con unidades disponibles (dentro de los 20 días).
 * - DEPLETED: se consumieron todas las unidades.
 * - EXPIRED:  pasaron los 20 días desde la apertura (haya o no unidades).
 */
public enum OpenVialStatus {
  OPEN,
  DEPLETED,
  EXPIRED
}
