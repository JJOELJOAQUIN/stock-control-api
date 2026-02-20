package com.jowi.stock.dashboard;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.cash.CashContext;
import com.jowi.stock.stock.StockContext;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  // =========================
  // RESUMEN GENERAL
  // =========================
  @GetMapping("/summary")
  public ResponseEntity<DashboardSummaryResponse> summary(
      @RequestParam StockContext context) {
    return ResponseEntity.ok(dashboardService.getSummary(context));
  }

  // =========================
  // KPIs MOVIMIENTOS STOCK
  // =========================
  @GetMapping("/movements")
  public ResponseEntity<StockMovementKpiResponse> movements() {
    return ResponseEntity.ok(dashboardService.getMovementKpis());
  }

  // =========================
  // KPIs CAJA 💰 (NUEVO)
  // =========================
  @GetMapping("/cash")
  public ResponseEntity<CashKpiSummaryResponse> cash() {
    return ResponseEntity.ok(dashboardService.getCashKpis());
  }

  @GetMapping("/cash/payment-method")
  public ResponseEntity<List<CashKpiByPaymentMethodResponse>> cashByPaymentMethod() {
    return ResponseEntity.ok(
        dashboardService.getCashByPaymentMethod());
  }

  @GetMapping("/cash/context")
  public ResponseEntity<CashKpiByContextResponse> cashByContext(
      @RequestParam CashContext context) {
    return ResponseEntity.ok(
        dashboardService.getCashByContext(context));
  }

  @GetMapping("/cash/monthly")
  public ResponseEntity<List<CashMonthlyKpiResponse>> cashMonthly(
      @RequestParam int year,
      @RequestParam(required = false) CashContext context) {
    return ResponseEntity.ok(
        dashboardService.getCashMonthly(year, context));
  }

  @GetMapping("/cash/compare")
  public ResponseEntity<CashKpiCompareResponse> cashCompare(
      @RequestParam int year,
      @RequestParam int month,
      @RequestParam(required = false) CashContext context) {
    return ResponseEntity.ok(
        dashboardService.getCashCompare(year, month, context));
  }

}
