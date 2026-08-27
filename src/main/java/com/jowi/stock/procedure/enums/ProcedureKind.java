package com.jowi.stock.procedure.enums;

/**
 * Familia del procedimiento, para agrupar en las vistas (las cards de
 * métricas y los modales de catálogo). El reparto NO se decide por acá sino
 * por ProcedureSplitRule; kind se deriva de la regla y viaja para el front.
 */
public enum ProcedureKind {
  MEDICA,
  COSMETOLOGIA;
}
