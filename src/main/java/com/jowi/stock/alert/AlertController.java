package com.jowi.stock.alert;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

  private final AlertService alertService;

  public AlertController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping("/low-stock")
  public ResponseEntity<List<AlertResponse>> lowStock() {
    return ResponseEntity.ok(alertService.lowStock());
  }

  @GetMapping("/out-of-stock")
  public ResponseEntity<List<AlertResponse>> outOfStock() {
    return ResponseEntity.ok(alertService.outOfStock());
  }
}
