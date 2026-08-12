package com.jowi.stock.procedure.enums;

/**
 * Cómo se consume el insumo de un tratamiento.
 *
 * - NONE        → receta fija (procedure_consumption): X de tal producto por
 *                 sesión, y el stock se descuenta solo al pasarla (suelto o en
 *                 venta combinada). Es lo normal.
 * - TOXINA_VIAL → flujo especial de vial: las unidades varían por sesión y hay
 *                 ciclo de vial (vencimiento, reuso FIFO). No usa receta fija;
 *                 dispara el diálogo de vial+unidades (sección de toxina y, más
 *                 adelante, también dentro de la venta combinada).
 */
public enum ProcedureSpecialFlow {
  NONE,
  TOXINA_VIAL;
}