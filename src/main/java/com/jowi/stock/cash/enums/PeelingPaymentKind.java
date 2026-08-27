package com.jowi.stock.cash.enums;

/**
 * Tipo de pago del protocolo de peeling profundo: pago completo o una de las
 * dos cuotas.
 *
 * Hasta ahora esto vivía sólo en el front (lib/peeling.ts) y llegaba al
 * backend como texto adentro del comment ("1ª cuota"). Viaja en el request
 * porque sin este dato el backend no puede validar que "Todo a Gise" sea
 * efectivamente la primera cuota: el monto no alcanza como señal, porque el
 * campo es editable en la UI.
 *
 * No se persiste: TODO_COSMETOLOGA ya implica FIRST, así que guardarlo sería
 * redundante. Si algún día un desvío puede aplicar a más de una cuota, esto
 * pasa a ser columna.
 */
public enum PeelingPaymentKind {
  FULL,
  FIRST,
  SECOND
}
