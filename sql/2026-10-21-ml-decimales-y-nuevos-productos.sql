-- =====================================================================
-- Datos 21/10/2026: NCTF en ml, PDRN, productos PBSERUM / SUISSELLE /
-- NAHYCO y tratamientos nuevos.
--
-- CORRER SOLO DESPUÉS del deploy de la API que pasa los ml a décimas
-- (migración "2026-10-ml-en-decimas"). Si se corre antes, el bloque de
-- control de abajo frena todo y no cambia nada.
--
-- Convención: las cantidades en ML se guardan en DÉCIMAS de ml
-- (1,5 ml = 15). La página siempre las muestra en ml reales.
-- =====================================================================
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM app_migrations WHERE name = '2026-10-ml-en-decimas') THEN
    RAISE EXCEPTION 'Falta el deploy de la API (migración 2026-10-ml-en-decimas). No se cambió nada.';
  END IF;
  IF EXISTS (SELECT 1 FROM products WHERE name = 'NCTF 135 HA') THEN
    RAISE EXCEPTION 'Este script ya se corrió (existe NCTF 135 HA). No se cambió nada.';
  END IF;
END $$;

-- ---------------------------------------------------------------------
-- 1) NCTF: de "dosis de 1,5 ml" a ml reales.
--    Todo lo guardado en dosis x15 (1 dosis = 1,5 ml = 15 décimas).
--    Caja = 5 viales x 3 ml = 15 ml. Ojeras 1 ml, ojeras + surcos 1,5 ml.
-- ---------------------------------------------------------------------
UPDATE stocks SET current_stock = current_stock * 15, version = version + 1
 WHERE product_id = (SELECT id FROM products WHERE name = 'NCTF 135 HA (dosis 1,5 ml)');
UPDATE product_batches SET quantity_current = quantity_current * 15, quantity_initial = quantity_initial * 15
 WHERE product_id = (SELECT id FROM products WHERE name = 'NCTF 135 HA (dosis 1,5 ml)');
UPDATE stock_movements SET quantity = quantity * 15
 WHERE product_id = (SELECT id FROM products WHERE name = 'NCTF 135 HA (dosis 1,5 ml)');
UPDATE stock_movement_batches SET quantity = quantity * 15
 WHERE batch_id IN (SELECT b.id FROM product_batches b JOIN products p ON p.id = b.product_id
                    WHERE p.name = 'NCTF 135 HA (dosis 1,5 ml)');
UPDATE products
   SET name = 'NCTF 135 HA', consumption_unit = 'ML', units_per_package = 150,
       minimum_stock = minimum_stock * 15, cost_price = 38760, updated_at = now()
 WHERE name = 'NCTF 135 HA (dosis 1,5 ml)';
UPDATE procedure_consumption SET quantity = 10, updated_at = now()   -- 1 ml
 WHERE procedure_code = 'OJERAS_NCTF';
UPDATE procedure_consumption SET quantity = 15, updated_at = now()   -- 1,5 ml
 WHERE procedure_code = 'OJERAS_SURCOS_NCTF';

-- ---------------------------------------------------------------------
-- 2) PDRN: vial de 4 ml a $93.500 ($23.375 el ml); la sesión usa 2 ml.
--    El Skinbooster PDRN no tenía receta: por eso nunca descontaba.
-- ---------------------------------------------------------------------
UPDATE products SET cost_price = 23375, updated_at = now()
 WHERE name = 'PDRN+ NAD+ SKINBOOSTER PLUS';
INSERT INTO procedure_consumption (id, procedure_code, product_id, quantity, created_at, updated_at)
SELECT gen_random_uuid(), 'SKINBOOSTER_REGENERATIVO_PDRN', id, 20, now(), now()   -- 2 ml
  FROM products WHERE name = 'PDRN+ NAD+ SKINBOOSTER PLUS';

-- ---------------------------------------------------------------------
-- 3) Productos nuevos.
--    PBSERUM: polvos que se diluyen en 1,5 ml de solución fisiológica.
--    Se cuentan en ml (1 polvo = 1,5 ml); costo $40.000 el polvo.
-- ---------------------------------------------------------------------
INSERT INTO products (id, created_at, updated_at, active, brand, category, cost_price, expirable,
                      minimum_stock, name, scope, restock_priority, consumption_unit, units_per_package)
VALUES
  (gen_random_uuid(), now(), now(), true, 'PBSERUM', 'MESOTERAPIA', 26666.67, true, 0,
   'LIPASA PB500 SLIM PBSERUM POLVO', 'CONSULTORIO', 0, 'ML', 15),
  (gen_random_uuid(), now(), now(), true, 'PBSERUM', 'MESOTERAPIA', 26666.67, true, 0,
   'COLLAGENASA GH PB220 PBSERUM POLVO', 'CONSULTORIO', 0, 'ML', 15),
  (gen_random_uuid(), now(), now(), true, 'PBSERUM', 'MESOTERAPIA', 26666.67, true, 0,
   'HYALURONIDASA PB3000 DRAN PBSERUM POLVO', 'CONSULTORIO', 0, 'ML', 15),
  (gen_random_uuid(), now(), now(), true, 'SUISSELLE', 'MESOTERAPIA', NULL, true, 0,
   'CELLBOOSTER SHAPE AMPOLLA', 'CONSULTORIO', 0, 'AMPOLLA', NULL),
  (gen_random_uuid(), now(), now(), true, 'NAHYCO', 'MESOTERAPIA', NULL, true, 0,
   'PROFHILO', 'CONSULTORIO', 0, 'ML', 20);

-- Stock inicial de los polvos que ya hay: 2 lipasas (3 ml), 1 colagenasa
-- (1,5 ml), 1 hialuronidasa (1,5 ml). El resto arranca en 0.
INSERT INTO stocks (id, current_stock, version, product_id, context)
SELECT gen_random_uuid(),
       CASE p.name WHEN 'LIPASA PB500 SLIM PBSERUM POLVO' THEN 30
                   WHEN 'COLLAGENASA GH PB220 PBSERUM POLVO' THEN 15
                   WHEN 'HYALURONIDASA PB3000 DRAN PBSERUM POLVO' THEN 15
                   ELSE 0 END,
       0, p.id, 'CONSULTORIO'
  FROM products p
 WHERE p.name IN ('LIPASA PB500 SLIM PBSERUM POLVO', 'COLLAGENASA GH PB220 PBSERUM POLVO',
                  'HYALURONIDASA PB3000 DRAN PBSERUM POLVO', 'CELLBOOSTER SHAPE AMPOLLA', 'PROFHILO');
INSERT INTO stock_movements (id, created_at, updated_at, quantity, product_id, type, reason_type, comment, context)
SELECT gen_random_uuid(), now(), now(), s.current_stock, s.product_id, 'IN', 'AJUSTE_ERROR',
       'Stock inicial (alta del producto)', 'CONSULTORIO'
  FROM stocks s JOIN products p ON p.id = s.product_id
 WHERE p.brand = 'PBSERUM' AND s.current_stock > 0;

-- ---------------------------------------------------------------------
-- 4) Tratamientos.
-- ---------------------------------------------------------------------
-- Cicatrices con lipasa + colagenasa + hialuronidasa: $80.000, la hace la
-- médica. La receta (cuántos ml de cada polvo por sesión) se carga desde
-- Catálogo de tratamientos cuando esté confirmada.
INSERT INTO procedure_catalog (id, created_at, updated_at, active, amount, code, label, kind,
                               performed_by, cosmetologist_percent, doctor_percent, special_flow)
VALUES (gen_random_uuid(), now(), now(), true, 80000, 'MESOTERAPIA_CICATRICES_ENZIMAS',
        'MESOTERAPIA CICATRICES (LIPASA + COLAGENASA + HIALURONIDASA)', 'MEDICA', 'MEDICA', 0, 1, 'NONE'),
       (gen_random_uuid(), now(), now(), true, NULL, 'SKINBOOSTER_PROFHILO',
        'SKINBOOSTER PROFHILO', 'MEDICA', 'MEDICA', 0, 1, 'NONE');

-- Cellbooster Shape = mesoterapia de adiposidades faciales; usa 1 ampolla.
UPDATE procedure_catalog SET label = 'MESOTERAPIA ADIPOSIDADES FACIALES (CELLBOOSTER SHAPE)', updated_at = now()
 WHERE code = 'CELLBOOSTER_SHAPE';
INSERT INTO procedure_consumption (id, procedure_code, product_id, quantity, created_at, updated_at)
SELECT gen_random_uuid(), 'CELLBOOSTER_SHAPE', id, 1, now(), now()
  FROM products WHERE name = 'CELLBOOSTER SHAPE AMPOLLA';

COMMIT;
