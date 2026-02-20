package com.jowi.stock.stock;

import com.jowi.stock.movement.*;
import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductService;

import jakarta.persistence.OptimisticLockException;

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

  private void validateScope(Product product, StockContext context) {

    if (product.getScope() == null) {
      throw new IllegalStateException("Product scope not configured");
    }

    switch (product.getScope()) {

      case BOTH:
        return;

      case LOCAL:
        if (context != StockContext.LOCAL) {
          throw new IllegalStateException("Product allowed only in LOCAL");
        }
        break;

      case CONSULTORIO:
        if (context != StockContext.CONSULTORIO) {
          throw new IllegalStateException("Product allowed only in CONSULTORIO");
        }
        break;
    }
  }

  public void initStock(UUID productId, StockContext context, int initialStock) {

    if (initialStock < 0) {
      throw new IllegalArgumentException("Initial stock cannot be negative");
    }

    Product product = productService.getById(productId);
    validateScope(product, context);

    if (stockRepository.existsByProductIdAndContext(productId, context)) {
      throw new IllegalStateException("Stock already initialized");
    }

    stockRepository.save(productId, context, initialStock);

    if (initialStock > 0) {
      movementService.register(
          productId,
          context,
          StockMovementType.ADJUST,
          initialStock,
          StockMovementReason.AJUSTE_ERROR,
          "Initial stock");
    }

  }

  public Stock getStock(UUID productId, StockContext context) {
    return stockRepository
        .findByProductIdAndContext(productId, context)
        .orElseThrow(() -> new IllegalStateException("Stock not found"));
  }

  public void increase(UUID productId, StockContext context, int qty) {

    validateQty(qty);

    Product product = productService.getById(productId);
    validateScope(product, context);

    Stock stock = getStock(productId, context);

    stockRepository.save(productId, context, stock.getCurrent() + qty);

    movementService.register(
        productId,
        context,
        StockMovementType.IN,
        qty,
        StockMovementReason.COMPRA_PROVEEDOR,
        "Ingreso de stock");
  }

  public List<LowStockResponse> getBelowMinimum(StockContext context) {

    return stockRepository.findAllByContext(context).stream()
        .filter(stock -> stock.isBelowMinimum())
        .map(LowStockResponse::from)
        .collect(Collectors.toList());
  }

  public void decrease(UUID productId, StockContext context, int qty) {

    validateQty(qty);

    Product product = productService.getById(productId);
    validateScope(product, context);

    Stock stock = getStock(productId, context);

    int newValue = stock.getCurrent() - qty;

    if (newValue < 0) {
      throw new IllegalStateException("Insufficient stock");
    }

    try {
      stockRepository.save(productId, context, newValue);
    } catch (OptimisticLockException e) {
      throw new IllegalStateException("Concurrent stock modification detected");
    }

    movementService.register(
        productId,
        context,
        StockMovementType.OUT,
        qty,
        StockMovementReason.VENTA,
        "Salida de stock");
  }

  public boolean exists(UUID productId, StockContext context) {
    return stockRepository.existsByProductIdAndContext(productId, context);
  }

  private void validateQty(Integer qty) {
    if (qty == null || qty <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
  }

}
