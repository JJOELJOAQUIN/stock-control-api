package com.jowi.stock.metrics.controllers;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.metrics.dto.MonthlyMetricsResponse;
import com.jowi.stock.metrics.services.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

  private final MetricsService service;

  public MetricsController(MetricsService service) {
    this.service = service;
  }

  @GetMapping("/monthly")
  public ResponseEntity<MonthlyMetricsResponse> monthly(
      @RequestParam CashContext context,
      @RequestParam int year,
      @RequestParam int month) {
    return ResponseEntity.ok(service.monthly(context, year, month));
  }
}