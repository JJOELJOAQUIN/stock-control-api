package com.jowi.stock.stock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.services.StockMovementService;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.enums.ConsumptionUnit;
import com.jowi.stock.product.enums.ProductScope;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.dto.ReconcileStockResponse;
import com.jowi.stock.stock.entities.Stock;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.exceptions.InsufficientStockException;
import com.jowi.stock.stock.repositories.StockRepository;
import com.jowi.stock.stock.services.StockService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockReconcileTest {

  private final UUID productId = UUID.randomUUID();
  private final StockContext ctx = StockContext.CONSULTORIO;

  private StockRepository stockRepository;
  private StockMovementService movementService;
  private ProductBatchService batchService;
  private StockService stockService;

  @BeforeEach
  void setUp() {
    stockRepository = mock(StockRepository.class);
    ProductService productService = mock(ProductService.class);
    movementService = mock(StockMovementService.class);
    batchService = mock(ProductBatchService.class);
    stockService = new StockService(stockRepository, productService, movementService, batchService);

    Product product = mock(Product.class);
    when(product.getId()).thenReturn(productId);
    when(product.getName()).thenReturn("EXOSOMAS");
    when(product.getScope()).thenReturn(ProductScope.CONSULTORIO);
    when(product.getConsumptionUnit()).thenReturn(ConsumptionUnit.ML);
    when(product.getUnitsPerPackage()).thenReturn(5);
    when(productService.getById(productId)).thenReturn(product);
  }

  private void givenStock(int current) {
    when(stockRepository.existsByProductIdAndContext(productId, ctx)).thenReturn(true);
    when(stockRepository.findByProductIdAndContext(productId, ctx))
        .thenReturn(Optional.of(new Stock(productId, current, 0)));
  }

  @Test
  void decreaseSinStockTiraConDatosDelProducto() {
    givenStock(1);

    InsufficientStockException ex = assertThrows(
        InsufficientStockException.class,
        () -> stockService.decrease(productId, ctx, 3, StockMovementReason.PROCEDIMIENTO, "x"));

    assertEquals(productId, ex.getProductId());
    assertEquals("EXOSOMAS", ex.getProductName());
    assertEquals(1, ex.getAvailable());
    assertEquals(3, ex.getRequested());
    assertEquals("ML", ex.getUnit());
    assertEquals(5, ex.getUnitsPerPackage());
    verify(stockRepository, never()).save(any(), any(), anyInt());
  }

  @Test
  void decreaseSinFilaDeStockEsInsuficienteConCero() {
    when(stockRepository.findByProductIdAndContext(productId, ctx)).thenReturn(Optional.empty());

    InsufficientStockException ex = assertThrows(
        InsufficientStockException.class,
        () -> stockService.decrease(productId, ctx, 1, StockMovementReason.PROCEDIMIENTO, "x"));

    assertEquals(0, ex.getAvailable());
  }

  @Test
  void reconcileHaciaArribaRegistraIngresoPorAjuste() {
    givenStock(0);

    ReconcileStockResponse r = stockService.reconcile(productId, ctx, 10, "conteo heladera");

    assertEquals(0, r.previous());
    assertEquals(10, r.current());
    assertEquals(10, r.difference());
    verify(stockRepository).save(productId, ctx, 10);
    verify(movementService).register(
        eq(productId), eq(ctx), eq(StockMovementType.IN), eq(10),
        eq(StockMovementReason.AJUSTE_ERROR), contains("conteo heladera"));
    verifyNoInteractions(batchService);
  }

  @Test
  void reconcileHaciaAbajoDescuentaLotes() {
    givenStock(8);
    when(batchService.consume(productId, ctx, 3)).thenReturn(List.of());

    ReconcileStockResponse r = stockService.reconcile(productId, ctx, 5, null);

    assertEquals(-3, r.difference());
    verify(stockRepository).save(productId, ctx, 5);
    verify(batchService).consume(productId, ctx, 3);
    verify(movementService).register(
        eq(productId), eq(ctx), eq(StockMovementType.OUT), eq(3),
        eq(StockMovementReason.AJUSTE_ERROR), anyString(), anyList());
  }

  @Test
  void reconcileSinDiferenciaNoRegistraMovimiento() {
    givenStock(4);

    ReconcileStockResponse r = stockService.reconcile(productId, ctx, 4, null);

    assertEquals(0, r.difference());
    verifyNoInteractions(movementService);
  }

  @Test
  void reconcileInicializaStockSiNoExiste() {
    when(stockRepository.existsByProductIdAndContext(productId, ctx)).thenReturn(false);
    when(stockRepository.findByProductIdAndContext(productId, ctx))
        .thenReturn(Optional.of(new Stock(productId, 0, 0)));

    stockService.reconcile(productId, ctx, 2, null);

    verify(stockRepository).save(productId, ctx, 0);
    verify(stockRepository).save(productId, ctx, 2);
  }

  @Test
  void reconcileNegativoEsInvalido() {
    assertThrows(IllegalArgumentException.class,
        () -> stockService.reconcile(productId, ctx, -1, null));
  }
}
