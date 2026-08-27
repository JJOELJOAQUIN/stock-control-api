package com.jowi.stock.purchase.controllers;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.purchase.dto.MonthlyPurchasesResponse;
import com.jowi.stock.purchase.services.PurchasesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchasesController {

  private final PurchasesService service;

  public PurchasesController(PurchasesService service) {
    this.service = service;
  }

  @GetMapping("/monthly")
  public ResponseEntity<MonthlyPurchasesResponse> monthly(
      @RequestParam CashContext context,
      @RequestParam int year,
      @RequestParam int month) {
    return ResponseEntity.ok(service.monthly(context, year, month));
  }
}
