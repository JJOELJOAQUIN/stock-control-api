package com.jowi.stock.treatment.enums;

/**
 * Estado de pago de un tratamiento. Se calcula a partir del total y la suma
 * de pagos registrados; no se setea a mano.
 */
public enum TreatmentStatus {
  PENDIENTE,
  PARCIAL,
  COMPLETO
}