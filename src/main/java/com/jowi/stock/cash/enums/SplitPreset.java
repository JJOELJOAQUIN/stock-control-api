package com.jowi.stock.cash.enums;

/**
 * Reparto de un pago de PEELING_PROFUNDO_PROTOCOLO.
 *
 * NORMAL           -> el reparto propio del peeling: monto fijo para la
 *                     cosmetóloga (hoy $40.000), y $0 en la segunda cuota.
 *                     OJO: NO es el 70/30 del catálogo de cosmetología; el
 *                     peeling nunca usó porcentajes.
 * TODO_COSMETOLOGA -> el neto entero va a la cosmetóloga. Sólo válido en la
 *                     primera cuota (ver validación en CashMovementService).
 * TODO_MEDICA      -> el neto entero va a la médica.
 *
 * Los desvíos (todo lo que no sea NORMAL) NO significan que el reparto real
 * del peeling haya cambiado: significan que hay una deuda entre Pili y Gise
 * saldándose por adentro del pago. Esta columna registra el hecho; no lo
 * explica. Es el dato que va a permitir migrar a una cuenta corriente el día
 * que haga falta.
 */
public enum SplitPreset {
  NORMAL,
  TODO_COSMETOLOGA,
  TODO_MEDICA
}