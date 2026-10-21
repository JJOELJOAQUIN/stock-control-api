package com.jowi.stock.common.migration;

import com.jowi.stock.product.enums.ConsumptionUnit;
import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.services.UnitRescaleService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Migraciones de datos que corren al arrancar la API, una sola vez cada una
 * (quedan registradas en la tabla app_migrations).
 *
 * El esquema lo maneja Hibernate (ddl-auto=update), pero hay dos cosas que
 * Hibernate no hace: cambiar datos existentes y actualizar las CHECK
 * constraints de los enums. Eso vive acá.
 */
@Component
@Profile("!test")
public class DataMigrations implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DataMigrations.class);

  static final String ML_TENTHS = "2026-10-ml-en-decimas";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final UnitRescaleService rescaler;

  public DataMigrations(JdbcTemplate jdbc, TransactionTemplate tx, UnitRescaleService rescaler) {
    this.jdbc = jdbc;
    this.tx = tx;
    this.rescaler = rescaler;
  }

  @Override
  public void run(ApplicationArguments args) {
    jdbc.execute("CREATE TABLE IF NOT EXISTS app_migrations ("
        + "name VARCHAR(100) PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())");

    syncBrandConstraint();
    runOnce(ML_TENTHS, this::mlToTenths);
  }

  /**
   * La CHECK de products.brand la generó Hibernate con las marcas de ese
   * momento y no se actualiza sola: una marca nueva en el enum (PBSERUM)
   * fallaba al guardar. Se regenera en cada arranque desde el enum.
   */
  void syncBrandConstraint() {
    String values = Arrays.stream(ProductBrand.values())
        .map(b -> "'" + b.name() + "'")
        .collect(Collectors.joining(", "));
    jdbc.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS products_brand_check");
    jdbc.execute("ALTER TABLE products ADD CONSTRAINT products_brand_check CHECK (brand IN ("
        + values + "))");
  }

  /**
   * ML pasa a guardarse en décimas de ml (ver ConsumptionUnit), para poder
   * descontar 1,5 ml o reconciliar un vial abierto con 2 ml.
   *
   * - Productos en ML SIN unitsPerPackage: son frascos de reventa a los que
   *   se les puso ML por error (su stock cuenta frascos, se venden por
   *   unidad). Pasan a UNIDAD, sin tocar cantidades.
   * - El resto de ML: todas sus cantidades x10.
   */
  void mlToTenths() {
    int toUnit = jdbc.update("UPDATE products SET consumption_unit = 'UNIDAD' "
        + "WHERE consumption_unit = 'ML' AND units_per_package IS NULL");

    List<UUID> ml = jdbc.queryForList(
        "SELECT id FROM products WHERE consumption_unit = 'ML'", UUID.class);
    int scale = ConsumptionUnit.ML.scale();
    for (UUID id : ml) {
      rescaler.rescale(id, 1, scale, true);
    }
    log.info("Migración {}: {} productos ML -> UNIDAD, {} productos re-escalados a décimas de ml",
        ML_TENTHS, toUnit, ml.size());
  }

  private void runOnce(String name, Runnable migration) {
    tx.executeWithoutResult(status -> {
      // Lock de la fila para que dos instancias arrancando a la vez no la
      // corran dos veces: la segunda espera y ve que ya está aplicada.
      int inserted = jdbc.update(
          "INSERT INTO app_migrations (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
      if (inserted == 0) return;
      migration.run();
    });
  }
}
