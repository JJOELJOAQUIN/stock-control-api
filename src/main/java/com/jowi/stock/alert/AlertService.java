package com.jowi.stock.alert;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductRepository;
import com.jowi.stock.stock.JpaStockRepository;
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
  public List<AlertResponse> lowStock() {
    return stockRepository.findBelowMinimum()
        .stream()
        .map(this::toLowStockAlert)
        .collect(Collectors.toList());
  }

  // =========================
  // ALERTAS SIN STOCK
  // =========================
  public List<AlertResponse> outOfStock() {
    return stockRepository.findByCurrent(0)
        .stream()
        .map(this::toOutOfStockAlert)
        .collect(Collectors.toList());
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
        "Stock por debajo del mínimo (" + stock.getCurrent() + "/" + stock.getMinimum() + ")",
        OffsetDateTime.now()
    );
  }

  private AlertResponse toOutOfStockAlert(StockEntity stock) {
    Product product = productRepository
        .findById(stock.getProductId())
        .orElseThrow();

    return new AlertResponse(
        AlertType.OUT_OF_STOCK,
        product.getId(),
        product.getName(),
        "Producto sin stock",
        OffsetDateTime.now()
    );
  }
}
