package com.jowi.stock.stock;

import com.jowi.stock.movement.*;
import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

  private final StockRepository stockRepository;
  private final ProductService productService;
  private final StockMovementService movementService;

  public StockService(
      StockRepository stockRepository,
      ProductService productService,
      StockMovementService movementService) {
    this.stockRepository = stockRepository;
    this.productService = productService;
    this.movementService = movementService;
  }

  public void initStock(UUID productId, int initialStock) {

    if (initialStock < 0) {
      throw new IllegalArgumentException("Initial stock cannot be negative");
    }

    Product product = productService.getById(productId);

    if (stockRepository.existsByProductId(productId)) {
      throw new IllegalStateException("Stock already initialized");
    }

    Stock stock = new Stock(
        productId,
        initialStock,
        product.getMinimumStock());

    stockRepository.save(stock);

    movementService.register(
        productId,
        StockMovementType.ADJUST,
        initialStock,
        StockMovementReason.AJUSTE_ERROR,
        "Initial stock");
  }

  public Stock getStock(UUID productId) {
    return stockRepository.findByProductId(productId)
        .orElseThrow(() -> new IllegalStateException("Stock not found for product " + productId));
  }

  public void increase(UUID productId, int qty) {

    validateQty(qty);

    Stock stock = stockRepository.findByProductId(productId).orElseThrow();

    Stock updated = new Stock(
        stock.getProductId(),
        stock.getCurrent() + qty,
        stock.getMinimum());

    stockRepository.save(updated);

    movementService.register(
        productId,
        StockMovementType.IN,
        qty,
        StockMovementReason.COMPRA_PROVEEDOR,
        "Ingreso de stock");
  }

  public void decrease(UUID productId, int qty) {

    validateQty(qty);

    Stock stock = stockRepository.findByProductId(productId).orElseThrow();

    int newValue = stock.getCurrent() - qty;
    if (newValue < 0) {
      throw new IllegalStateException("Insufficient stock");
    }

    Stock updated = new Stock(
        stock.getProductId(),
        newValue,
        stock.getMinimum());

    stockRepository.save(updated);

    movementService.register(
        productId,
        StockMovementType.OUT,
        qty,
        StockMovementReason.VENTA,
        "Salida de stock");
  }


    // =========================
  // STOCK BAJO MINIMO
  // =========================
  public List<LowStockResponse> getBelowMinimum() {

    return stockRepository.findAll().stream()
        .map(e -> new Stock(
            e.getProductId(),
            e.getCurrent(),
            e.getMinimum()
        ))
        .filter(Stock::isBelowMinimum)
        .map(LowStockResponse::from)
        .collect(Collectors.toList());
  }

  private void validateQty(int qty) {
    if (qty <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
  }
}
