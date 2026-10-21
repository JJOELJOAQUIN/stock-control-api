package com.jowi.stock.product.services;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-escala TODAS las cantidades guardadas de un producto cuando cambia la
 * escala de su unidad (ver ConsumptionUnit): stock, lotes, movimientos,
 * detalle por lote y recetas. Ej. pasar un producto a ML (escala 10)
 * multiplica todo por 10 para que 3 ml sigan siendo 3 ml.
 *
 * Va por SQL directo porque toca el histórico completo de una vez y no hay
 * reglas de negocio que aplicar: es la misma cantidad expresada en otra
 * unidad. Bajar de escala redondea y nunca deja en 0 lo que las
 * constraints exigen >= 1 (movimientos, recetas).
 */
@Service
public class UnitRescaleService {

  private final JdbcTemplate jdbc;

  public UnitRescaleService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Multiplica las cantidades del producto por {@code to / from}.
   *
   * @param includeProductFields true también re-escala minimum_stock y
   *     units_per_package (migración); false cuando esos campos ya vienen en
   *     la escala nueva (edición del producto desde el front).
   */
  @Transactional
  public void rescale(UUID productId, int from, int to, boolean includeProductFields) {
    if (from == to) return;
    String f = "ROUND(%s * " + to + " / " + from + ".0)";
    String atLeastOne = "GREATEST(1, " + f + ")";

    jdbc.update("UPDATE stocks SET current_stock = " + f.formatted("current_stock")
        + ", version = version + 1 WHERE product_id = ?", productId);
    jdbc.update("UPDATE product_batches SET quantity_current = " + f.formatted("quantity_current")
        + ", quantity_initial = " + f.formatted("quantity_initial") + " WHERE product_id = ?", productId);
    jdbc.update("UPDATE stock_movements SET quantity = " + atLeastOne.formatted("quantity")
        + " WHERE product_id = ?", productId);
    jdbc.update("UPDATE stock_movement_batches SET quantity = " + atLeastOne.formatted("quantity")
        + " WHERE batch_id IN (SELECT id FROM product_batches WHERE product_id = ?)", productId);
    jdbc.update("UPDATE procedure_consumption SET quantity = " + atLeastOne.formatted("quantity")
        + " WHERE product_id = ?", productId);

    if (includeProductFields) {
      jdbc.update("UPDATE products SET minimum_stock = " + f.formatted("minimum_stock")
          + ", units_per_package = CASE WHEN units_per_package IS NULL THEN NULL ELSE "
          + atLeastOne.formatted("units_per_package") + " END WHERE id = ?", productId);
    }
  }
}
