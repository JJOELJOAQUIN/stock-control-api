package com.jowi.stock.alert;

import com.jowi.stock.product.Product;
import com.jowi.stock.stock.JpaStockRepository;
import com.jowi.stock.stock.StockContext;
import com.jowi.stock.stock.StockEntity;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertService {

  private final JpaStockRepository stockRepository;

  public AlertService(JpaStockRepository stockRepository) {
    this.stockRepository = stockRepository;

  }

  // =========================
  // ALERTAS STOCK BAJO
  // =========================
  public List<AlertResponse> lowStock(StockContext context) {
    return stockRepository.findByContext(context)
        .stream()
        .filter(stock -> stock.getCurrent() < stock.getProduct().getMinimumStock())
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
    Product product = stock.getProduct();

    return new AlertResponse(
        AlertType.LOW_STOCK,
        product.getId(),
        product.getName(),
        "Stock por debajo del mínimo (" + stock.getCurrent() + "/" + product.getMinimumStock() + ")",
        OffsetDateTime.now());
  }

  private AlertResponse toOutOfStockAlert(StockEntity stock) {
    Product product = stock.getProduct();

    return new AlertResponse(
        AlertType.OUT_OF_STOCK,
        product.getId(),
        product.getName(),
        "Producto sin stock",
        OffsetDateTime.now());
  }
}