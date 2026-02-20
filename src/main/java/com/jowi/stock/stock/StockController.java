package com.jowi.stock.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockController {

  private final StockService stockService;

  public StockController(StockService stockService) {
    this.stockService = stockService;
  }

  // =========================
  // INIT STOCK
  // =========================
  @PostMapping("/{productId}/init")
  public ResponseEntity<Void> initStock(
      @PathVariable UUID productId,
      @RequestParam StockContext context,
      @RequestParam @NotNull @Min(0) Integer initialStock) {

    stockService.initStock(productId, context, initialStock);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // =========================
  // GET STOCK
  // =========================
  @GetMapping("/{productId}")
  public ResponseEntity<StockResponse> getStock(
      @PathVariable UUID productId,
      @RequestParam StockContext context) {

    Stock stock = stockService.getStock(productId, context);
    return ResponseEntity.ok(StockResponse.from(stock));
  }

  // =========================
  // STOCK CRITICO
  // =========================
  @GetMapping("/below-minimum")
  public ResponseEntity<List<LowStockResponse>> getBelowMinimum(
      @RequestParam StockContext context) {

    return ResponseEntity.ok(
        stockService.getBelowMinimum(context));
  }

  // =========================
  // INCREASE
  // =========================
  @PostMapping("/{productId}/in")
  public ResponseEntity<Void> increase(
      @PathVariable UUID productId,
      @RequestParam StockContext context,
      @RequestParam @NotNull @Min(1) Integer qty) {

    stockService.increase(productId, context, qty);
    return ResponseEntity.ok().build();
  }

  // =========================
  // DECREASE
  // =========================
  @PostMapping("/{productId}/out")
  public ResponseEntity<Void> decrease(
      @PathVariable UUID productId,
      @RequestParam StockContext context,
      @RequestParam @NotNull @Min(1) Integer qty) {

    stockService.decrease(productId, context, qty);
    return ResponseEntity.ok().build();
  }
}
