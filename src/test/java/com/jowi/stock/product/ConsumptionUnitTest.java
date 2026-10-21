package com.jowi.stock.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jowi.stock.product.enums.ConsumptionUnit;
import org.junit.jupiter.api.Test;

class ConsumptionUnitTest {

  @Test
  void mlSeGuardaEnDecimas() {
    assertEquals(10, ConsumptionUnit.ML.scale());
    assertEquals("7,5 ml", ConsumptionUnit.format(75, ConsumptionUnit.ML));
    assertEquals("15 ml", ConsumptionUnit.format(150, ConsumptionUnit.ML));
    assertEquals("0 ml", ConsumptionUnit.format(0, ConsumptionUnit.ML));
  }

  @Test
  void elRestoSonEnteros() {
    assertEquals(1, ConsumptionUnit.scaleOf(null));
    assertEquals("18 amp.", ConsumptionUnit.format(18, ConsumptionUnit.AMPOLLA));
    assertEquals("3 u.", ConsumptionUnit.format(3, null));
  }
}
