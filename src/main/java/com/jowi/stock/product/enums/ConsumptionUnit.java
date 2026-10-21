package com.jowi.stock.product.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unidad en la que se CUENTA el stock de un producto. Para el retail es
 * UNIDAD (como siempre). Para los insumos de procedimientos es la unidad
 * consumible: ML (viales que se fraccionan por sesión, ej. NCTF), AMPOLLA
 * (viales de un solo uso, ej. DMAE) o DISPARO (pines de Frax Face).
 *
 * El motor de stock/lotes trabaja SIEMPRE con enteros. Para que los ml
 * admitan fracciones (sesión de 1,5 ml, vial abierto con 2 ml), cada unidad
 * tiene una escala: el entero guardado es la cantidad x escala. ML guarda
 * DÉCIMAS de ml (escala 10: 1,5 ml -> 15); el resto, unidades enteras.
 *
 * Todo lo que guarda cantidades de un producto en ML va en décimas: stock,
 * lotes, movimientos, recetas, mínimo y unitsPerPackage (caja NCTF 15 ml ->
 * 150). El costo (costPrice) en cambio es por unidad visible (por ml).
 * Al usuario se le muestra y se le pide siempre en ml, nunca en décimas.
 */
public enum ConsumptionUnit {
  UNIDAD(1, "u."),
  ML(10, "ml"),
  AMPOLLA(1, "amp."),
  DISPARO(1, "disp.");

  private final int scale;
  private final String label;

  ConsumptionUnit(int scale, String label) {
    this.scale = scale;
    this.label = label;
  }

  /** Cuántos enteros guardados equivalen a 1 unidad visible (ML: 10). */
  public int scale() {
    return scale;
  }

  public String label() {
    return label;
  }

  /** Escala de una unidad posiblemente null (null = UNIDAD). */
  public static int scaleOf(ConsumptionUnit unit) {
    return unit == null ? 1 : unit.scale;
  }

  /** Cantidad guardada -> texto para el usuario: 75 en ML -> "7,5 ml". */
  public static String format(int stored, ConsumptionUnit unit) {
    ConsumptionUnit u = unit == null ? UNIDAD : unit;
    BigDecimal visible = BigDecimal.valueOf(stored)
        .divide(BigDecimal.valueOf(u.scale), 1, RoundingMode.HALF_UP)
        .stripTrailingZeros();
    String number = visible.scale() < 0 ? visible.setScale(0).toPlainString() : visible.toPlainString();
    return number.replace('.', ',') + " " + u.label;
  }
}
