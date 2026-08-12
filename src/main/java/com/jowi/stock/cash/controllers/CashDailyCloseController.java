package com.jowi.stock.cash.controllers;

import com.jowi.stock.cash.dto.CloseCashRequest;
import com.jowi.stock.cash.dto.DailyCashSummaryResponse;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.services.CashDailyCloseService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Cierre de caja diario. Lo maneja la médica (ADMIN): ve efectivo vs
 * transferencia del día y lo cierra. La cosmetóloga no accede a los totales
 * globales de caja.
 */
@RestController
@RequestMapping("/api/cash/close")
@PreAuthorize("hasRole('ADMIN')")
public class CashDailyCloseController {

  private final CashDailyCloseService service;

  public CashDailyCloseController(CashDailyCloseService service) {
    this.service = service;
  }

  /** Preview en vivo del día (por defecto hoy). */
  @GetMapping("/preview")
  public ResponseEntity<DailyCashSummaryResponse> preview(
      @RequestParam CashContext context,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(service.preview(context, date));
  }

  /** Cierra la caja del día. */
  @PostMapping
  public ResponseEntity<DailyCashSummaryResponse> close(
      @Valid @RequestBody CloseCashRequest req) {
    return ResponseEntity.ok(service.close(req.context(), req.date(), req.note()));
  }

  /** Historial de cierres del contexto, del más reciente al más viejo. */
  @GetMapping("/history")
  public ResponseEntity<List<DailyCashSummaryResponse>> history(
      @RequestParam CashContext context) {
    return ResponseEntity.ok(service.history(context));
  }
}