package com.jowi.stock.cash;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
      Pageable pageable
  ) {
    Page<CashMovement> page = (context == null)
        ? service.list(pageable)
        : service.listByContext(context, pageable);

    return ResponseEntity.ok(page.map(CashMovementResponse::from));
  }
}
