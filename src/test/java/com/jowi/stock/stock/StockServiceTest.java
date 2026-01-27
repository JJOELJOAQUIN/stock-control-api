package com.jowi.stock.stock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.jowi.stock.movement.StockMovementService;

class StockServiceTest {

  private StockRepository stockRepository;
  private ProductService productService;
  private StockService stockService;

  @BeforeEach
  void setUp() {
    stockRepository = Mockito.mock(StockRepository.class);
    productService = Mockito.mock(ProductService.class);
    StockMovementService movementService = mock(StockMovementService.class);

    stockService = new StockService(
        stockRepository,
        productService,
        movementService);

  }

  @Test
  void shouldInitializeStockSuccessfully() {
    UUID productId = UUID.randomUUID();

    Product product = new Product();
    product.setMinimumStock(5);

    when(productService.getById(productId)).thenReturn(product);
    when(stockRepository.existsByProductId(productId)).thenReturn(false);

    stockService.initStock(productId, 10);

    ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(captor.capture());

    Stock saved = captor.getValue();
    assertEquals(productId, saved.getProductId());
    assertEquals(10, saved.getCurrent());
    assertEquals(5, saved.getMinimum());
  }

  @Test
  void shouldNotAllowNegativeInitialStock() {
    UUID productId = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () -> stockService.initStock(productId, -1));

    verifyNoInteractions(stockRepository);
  }

  @Test
  void shouldIncreaseStock() {
    UUID productId = UUID.randomUUID();

    Stock existing = new Stock(productId, 5, 2);

    when(stockRepository.findByProductId(productId))
        .thenReturn(Optional.of(existing));

    stockService.increase(productId, 3);

    ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(captor.capture());

    Stock updated = captor.getValue();
    assertEquals(8, updated.getCurrent());
  }

  @Test
  void shouldDecreaseStock() {
    UUID productId = UUID.randomUUID();

    Stock existing = new Stock(productId, 10, 2);

    when(stockRepository.findByProductId(productId))
        .thenReturn(Optional.of(existing));

    stockService.decrease(productId, 4);

    ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(captor.capture());

    Stock updated = captor.getValue();
    assertEquals(6, updated.getCurrent());
  }

  @Test
  void shouldThrowWhenDecreasingBelowZero() {
    UUID productId = UUID.randomUUID();

    Stock existing = new Stock(productId, 3, 1);

    when(stockRepository.findByProductId(productId))
        .thenReturn(Optional.of(existing));

    assertThrows(
        IllegalStateException.class,
        () -> stockService.decrease(productId, 5));

    verify(stockRepository, never()).save(any());
  }

  @Test
  void shouldRejectZeroOrNegativeQuantity() {
    UUID productId = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () -> stockService.increase(productId, 0));

    assertThrows(
        IllegalArgumentException.class,
        () -> stockService.decrease(productId, -1));
  }
}