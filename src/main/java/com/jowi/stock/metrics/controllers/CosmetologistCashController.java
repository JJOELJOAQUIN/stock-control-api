package com.jowi.stock.metrics.controllers;

import com.jowi.stock.metrics.dto.CosmetologistCashResponse;
import com.jowi.stock.metrics.dto.CreateCosmetologistCashRequest;
import com.jowi.stock.metrics.services.CosmetologistCashService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Efectivo que la cosmetóloga le deja a la médica (ADMIN y COSMETOLOGA). */
@RestController
@RequestMapping("/api/metrics/cosmetologist-cash")
public class CosmetologistCashController {

  private final CosmetologistCashService service;

  public CosmetologistCashController(CosmetologistCashService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<CosmetologistCashResponse> monthly(
      @RequestParam int year,
      @RequestParam int month) {
    return ResponseEntity.ok(service.monthly(year, month));
  }

  @PostMapping
  public ResponseEntity<CosmetologistCashResponse.Entry> create(
      @Valid @RequestBody CreateCosmetologistCashRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
