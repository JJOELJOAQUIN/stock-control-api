package com.jowi.stock.cash.controllers;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.cash.dto.CashDailySplitResponse;
import com.jowi.stock.cash.dto.CashCosmetologistSplitResponse;
import com.jowi.stock.cash.dto.CashMovementResponse;
import com.jowi.stock.cash.dto.CashSalesTotalsResponse;
import com.jowi.stock.cash.dto.CreateCashMovementRequest;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.services.CashMovementService;

@RestController
@RequestMapping("/api/cash-movements")
public class CashMovementController {

  private final CashMovementService service;

  public CashMovementController(CashMovementService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<CashMovementResponse> create(@Valid @RequestBody CreateCashMovementRequest req) {
    CashMovement created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(CashMovementResponse.from(created));
  }

  @GetMapping
  public ResponseEntity<Page<CashMovementResponse>> list(
      @RequestParam(required = false) CashContext context,
      Pageable pageable) {
    Page<CashMovement> page = (context == null)
        ? service.list(pageable)
        : service.listByContext(context, pageable);

    return ResponseEntity.ok(page.map(CashMovementResponse::from));
  }

  @GetMapping("/daily-split")
  public ResponseEntity<CashDailySplitResponse> dailySplit(
      @RequestParam CashContext context,
      @RequestParam(required = false) java.time.LocalDate date) {
    return ResponseEntity.ok(service.dailySplit(context, date));
  }

  @GetMapping("/daily-split/cosmetologist")
  public ResponseEntity<CashCosmetologistSplitResponse> cosmetologistDailySplit(
      @RequestParam CashContext context,
      @RequestParam(required = false) java.time.LocalDate date) {
    return ResponseEntity.ok(service.cosmetologistDailySplit(context, date));
  }

  @GetMapping("/sales-totals")
  public ResponseEntity<CashSalesTotalsResponse> salesTotals(
      @RequestParam CashContext context) {
    return ResponseEntity.ok(service.salesTotals(context));
  }
}