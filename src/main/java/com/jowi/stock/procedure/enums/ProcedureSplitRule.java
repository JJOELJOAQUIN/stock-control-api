package com.jowi.stock.procedure.enums;

/**
 * Las TRES formas de reparto válidas de un procedimiento. El ABM sólo deja
 * elegir entre estas: no hay porcentajes libres, así nadie puede crear un
 * cuarto reparto no documentado que después descuadre la caja.
 *
 * - MEDICA_100  → 100% médica, la hace la médica (dermatológicos de Pili).
 * - COSMO_70_30 → 70% cosmetóloga / 30% médica, la hace Gise (lo habitual).
 * - COSMO_50_50 → 50/50, la hace Gise (la excepción: FRAX con limpieza, etc.).
 *
 * Es la fuente de verdad del reparto: kind, performer y los dos porcentajes
 * se derivan de acá (ver ProcedureCatalogService), nunca se cargan sueltos.
 */
public enum ProcedureSplitRule {
  MEDICA_100,
  COSMO_70_30,
  COSMO_50_50;
}
