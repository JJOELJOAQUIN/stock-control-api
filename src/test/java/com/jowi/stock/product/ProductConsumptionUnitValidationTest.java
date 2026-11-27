package com.jowi.stock.product;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jowi.stock.procedure.repositories.ProcedureCatalogRepository;
import com.jowi.stock.procedure.repositories.ProcedureConsumptionRepository;
import com.jowi.stock.product.dto.CreateProductRequest;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.enums.ConsumptionUnit;
import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;
import com.jowi.stock.product.enums.ProductScope;
import com.jowi.stock.product.repositories.ProductRepository;
import com.jowi.stock.product.services.ProductServiceImpl;
import com.jowi.stock.product.services.UnitRescaleService;
import com.jowi.stock.stock.repositories.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Un producto de venta se cuenta por unidad; ml/ampolla/disparo son para
 * insumos. Caso real: la Pantalla Solar Gel x50grs se creó en ml y cada
 * envase comprado entró como 0,1 ml.
 */
class ProductConsumptionUnitValidationTest {

  private ProductRepository productRepository;
  private ProductServiceImpl service;

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    service = new ProductServiceImpl(
        productRepository,
        mock(StockRepository.class),
        mock(ProcedureConsumptionRepository.class),
        mock(ProcedureCatalogRepository.class),
        mock(UnitRescaleService.class));
  }

  private static CreateProductRequest request(
      ProductCategory category, ConsumptionUnit unit, Integer unitsPerPackage) {
    return new CreateProductRequest(
        "PANTALLA SOLAR SPF 30 GEL X 50GRS", "", 3, category, ProductBrand.LACROZE,
        false, ProductScope.CONSULTORIO, null, new BigDecimal("7737.20"), null, null,
        null, 0, unit, unitsPerPackage, List.of());
  }

  @Test
  void cosmeticoDeVentaEnMlSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> service.create(request(ProductCategory.COSMETICO_VENTA, ConsumptionUnit.ML, 500)));
    verify(productRepository, never()).save(any());
  }

  @Test
  void cosmeticoDeVentaEnAmpollaSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> service.create(request(ProductCategory.COSMETICO_VENTA, ConsumptionUnit.AMPOLLA, null)));
  }

  @Test
  void cosmeticoDeVentaPorUnidadOSinUnidadSeAcepta() {
    assertDoesNotThrow(
        () -> service.create(request(ProductCategory.COSMETICO_VENTA, ConsumptionUnit.UNIDAD, null)));
    assertDoesNotThrow(
        () -> service.create(request(ProductCategory.COSMETICO_VENTA, null, null)));
  }

  @Test
  void insumoEnMlSinContenidoDeEnvaseSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> service.create(request(ProductCategory.MESOTERAPIA, ConsumptionUnit.ML, null)));
  }

  @Test
  void insumoEnMlConContenidoSeAcepta() {
    assertDoesNotThrow(
        () -> service.create(request(ProductCategory.MESOTERAPIA, ConsumptionUnit.ML, 150)));
  }

  @Test
  void insumoEnAmpollaSinContenidoSeAcepta() {
    // Una jeringa/ampolla suelta: 1 envase = 1 ampolla, como PROFHILO.
    assertDoesNotThrow(
        () -> service.create(request(ProductCategory.MESOTERAPIA, ConsumptionUnit.AMPOLLA, null)));
  }
}
