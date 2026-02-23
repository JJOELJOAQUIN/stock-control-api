package com.jowi.stock.alert;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductRepository;
import com.jowi.stock.stock.JpaStockRepository;
import com.jowi.stock.stock.StockContext;
import com.jowi.stock.stock.StockEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertService {

  private final JpaStockRepository stockRepository;
  private final ProductRepository productRepository;

  public AlertService(
      JpaStockRepository stockRepository,
      ProductRepository productRepository) {
    this.stockRepository = stockRepository;
    this.productRepository = productRepository;
  }

  // =========================
  // ALERTAS STOCK BAJO
  // =========================
  public List<AlertResponse> lowStock(StockContext context) {

    return stockRepository.findByContext(context)
        .stream()
        .filter(stock -> {
          Product product = productRepository
              .findById( stock.getProduct().getMinimumStock())
              .orElseThrow();
          return stock.getCurrent() < product.getMinimumStock();
        })
        .map(this::toLowStockAlert)
        .toList();
  }

  // =========================
  // ALERTAS SIN STOCK
  // =========================
  public List<AlertResponse> outOfStock(StockContext context) {

    return stockRepository.findByContext(context)
        .stream()
        .filter(stock -> stock.getCurrent() == 0)
        .map(this::toOutOfStockAlert)
        .toList();
  }

  // =========================
  // HELPERS
  // =========================
  private AlertResponse toLowStockAlert(StockEntity stock) {
    Product product = productRepository
        .findById(stock.getProductId())
        .orElseThrow();

    return new AlertResponse(
        AlertType.LOW_STOCK,
        product.getId(),
        product.getName(),
        "Stock por debajo del mínimo (" +
            stock.getCurrent() + "/" + product.getMinimumStock() + ")",
        OffsetDateTime.now());
  }

  private AlertResponse toOutOfStockAlert(StockEntity stock) {
    Product product = productRepository
        .findById(stock.getProduct())
        .orElseThrow();

    return new AlertResponse(
        AlertType.OUT_OF_STOCK,
        product.getId(),
        product.getName(),
        "Producto sin stock",
        OffsetDateTime.now());
  }
}
