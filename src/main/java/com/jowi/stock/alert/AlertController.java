package com.jowi.stock.alert;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.stock.StockContext;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

  private final AlertService alertService;

  public AlertController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping("/low-stock")
  public ResponseEntity<List<AlertResponse>> lowStock(
      @RequestParam StockContext context) {
    return ResponseEntity.ok(alertService.lowStock(context));
  }

  @GetMapping("/out-of-stock")
  public ResponseEntity<List<AlertResponse>> outOfStock(
      @RequestParam StockContext context) {
    return ResponseEntity.ok(alertService.outOfStock(context));
  }

}
