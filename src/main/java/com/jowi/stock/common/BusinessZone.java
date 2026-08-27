package com.jowi.stock.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Zona horaria del negocio y armado de rangos de fecha para las agregaciones.
 *
 * FUENTE ÚNICA. Nunca usar ZoneId.systemDefault() para lógica de negocio con
 * fecha: la JVM corre en UTC en Railway y en la zona local en Docker, así que
 * systemDefault() arma los límites de día/mes distinto según dónde corra el
 * proceso. Un cobro de las 21:00–23:59 AR (00:00–02:59 UTC del día siguiente)
 * caía en el día —y a veces en el mes— equivocado, y la caja del día mostraba
 * plata que era de otra fecha.
 *
 * createdAt es un Instant (tiempo absoluto), así que el rango se calcula
 * resolviendo el inicio del día EN esta zona y llevándolo a Instant. Todas las
 * queries por rango del sistema tienen que pasar por acá.
 */
public final class BusinessZone {

  public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

  private BusinessZone() {}

  /** Hoy, en la zona del negocio (no en la de la JVM). */
  public static LocalDate today() {
    return LocalDate.now(ZONE);
  }

  /**
   * Rango [desde, hasta) de un día completo en hora Argentina, como Instant.
   * Semiabierto: incluye desde las 00:00 del día y excluye las 00:00 del
   * siguiente, que es lo que esperan las queries (createdAt >= from AND < to).
   */
  public static Range ofDay(LocalDate day) {
    Instant from = day.atStartOfDay(ZONE).toInstant();
    Instant to = day.plusDays(1).atStartOfDay(ZONE).toInstant();
    return new Range(from, to);
  }

  /** Rango [desde, hasta) de un mes completo en hora Argentina, como Instant. */
  public static Range ofMonth(int year, int month) {
    LocalDate first = YearMonth.of(year, month).atDay(1);
    Instant from = first.atStartOfDay(ZONE).toInstant();
    Instant to = first.plusMonths(1).atStartOfDay(ZONE).toInstant();
    return new Range(from, to);
  }

  /**
   * Inicio de un día (00:00 AR) como Instant. Para filtros opcionales donde
   * sólo se necesita un extremo del rango.
   */
  public static Instant startOfDay(LocalDate day) {
    return day.atStartOfDay(ZONE).toInstant();
  }

  /** Rango semiabierto [from, to) en tiempo absoluto. */
  public record Range(Instant from, Instant to) {}
}
