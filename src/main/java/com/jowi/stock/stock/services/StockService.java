package com.jowi.stock.stock.services;

import com.jowi.stock.batch.dto.BatchAllocation;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.services.StockMovementService;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.dto.LowStockResponse;
import com.jowi.stock.stock.entities.Stock;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.repositories.StockRepository;

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
  private final ProductBatchService batchService;

  public StockService(
      StockRepository stockRepository,
      ProductService productService,
      StockMovementService movementService,
      ProductBatchService batchService) {
    this.stockRepository = stockRepository;
    this.productService = productService;
    this.movementService = movementService;
    this.batchService = batchService;
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
    increase(productId, context, qty, StockMovementReason.COMPRA_PROVEEDOR,
        "Ingreso de stock", List.of());
  }

  /**
   * Entrada de stock con motivo explícito y trazabilidad opcional de lotes.
   * Las allocations se usan al revertir una venta: cada unidad vuelve al lote
   * del que salió, y queda registrado que volvió.
   */
  public StockMovement increase(
      UUID productId,
      StockContext context,
      int qty,
      StockMovementReason reason,
      String comment,
      List<BatchAllocation> allocations) {

    validateQty(qty);

    Product product = productService.getById(productId);
    validateScope(product, context);

    Stock stock = getStock(productId, context);

    stockRepository.save(productId, context, stock.getCurrent() + qty);

    return movementService.register(
        productId,
        context,
        StockMovementType.IN,
        qty,
        reason,
        comment,
        allocations == null ? List.of() : allocations);
  }

  public List<LowStockResponse> getBelowMinimum(StockContext context) {

    return stockRepository.findAllByContext(context).stream()
        .filter(stock -> stock.isBelowMinimum())
        .map(LowStockResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * Salida de stock por venta (comportamiento histórico, sin cambios de
   * firma para no romper llamadas existentes).
   */
  public StockMovement decrease(UUID productId, StockContext context, int qty) {
    return decrease(productId, context, qty, StockMovementReason.VENTA, "Salida de stock");
  }

  /**
   * Salida de stock con motivo y comentario explícitos. Usada por ventas
   * (motivo VENTA) y por consumos internos (USO_PERSONAL, USO_CAMILLA,
   * TRASLADO, MUESTRA, REGALO, PEDIDO_ESPECIAL, OTRO).
   *
   * Además de descontar el stock, consume los lotes en orden FEFO para que
   * los avisos de vencimiento reflejen solo lotes con existencia real, y deja
   * registrado de qué lote salió cada unidad para poder revertirlo después.
   *
   * Devuelve el movimiento creado: quien llama puede atarlo a un movimiento
   * de caja con {@code movementService.linkToCashMovement}.
   */
  public StockMovement decrease(
      UUID productId,
      StockContext context,
      int qty,
      StockMovementReason reason,
      String comment) {

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

    // Mantiene los lotes sincronizados con el stock (fix NCTF 130 HA:
    // antes los lotes nunca se descontaban y seguían apareciendo como
    // próximos a vencer aunque el producto ya no tuviera stock).
    List<BatchAllocation> allocations = batchService.consume(productId, context, qty);

    return movementService.register(
        productId,
        context,
        StockMovementType.OUT,
        qty,
        reason,
        comment,
        allocations);
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