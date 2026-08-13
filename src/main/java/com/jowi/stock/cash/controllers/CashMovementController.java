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
import com.jowi.stock.cash.dto.VoidCashMovementRequest;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.services.CashMovementService;
import com.jowi.stock.cash.services.CashVoidService;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.repositories.TreatmentRepository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cash-movements")
public class CashMovementController {

  private final CashMovementService service;
  private final CashVoidService voidService;
  private final TreatmentRepository treatmentRepository;

  public CashMovementController(
      CashMovementService service,
      CashVoidService voidService,
      TreatmentRepository treatmentRepository) {
    this.service = service;
    this.voidService = voidService;
    this.treatmentRepository = treatmentRepository;
  }

  @PostMapping
  public ResponseEntity<CashMovementResponse> create(@Valid @RequestBody CreateCashMovementRequest req) {
    CashMovement created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(CashMovementResponse.from(created));
  }

  @GetMapping
  public ResponseEntity<Page<CashMovementResponse>> list(
      @RequestParam(required = false) CashContext context,
      @RequestParam(required = false) CashMovementType type,
      @RequestParam(required = false) CashSource source,
      @RequestParam(required = false) java.time.LocalDate dateFrom,
      @RequestParam(required = false) java.time.LocalDate dateTo,
      @RequestParam(required = false) String q,
      Pageable pageable) {
    Page<CashMovement> page = service.search(context, type, source, dateFrom, dateTo, q, pageable);

    // Enriquecemos con el nombre del paciente para los pagos de tratamiento
    // (reference_id = treatmentId). Se resuelve en lote para no hacer N+1.
    var treatmentIds = page.getContent().stream()
        .filter(m -> m.getSource() == CashSource.PROCEDURE && m.getReferenceId() != null)
        .map(CashMovement::getReferenceId)
        .collect(Collectors.toSet());

    Map<UUID, String> names = treatmentIds.isEmpty()
        ? Map.of()
        : treatmentRepository.findAllById(treatmentIds).stream()
            .collect(Collectors.toMap(
                Treatment::getId,
                t -> (t.getPatient().getFirstName() + " " + t.getPatient().getLastName()).trim()));

    return ResponseEntity.ok(page.map(m -> CashMovementResponse.from(
        m,
        m.getReferenceId() == null ? null : names.get(m.getReferenceId()))));
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

  /**
   * Anula un movimiento (soft delete visible). Disponible para las dos: el
   * registro guarda quién fue, que es la garantía real. Compras: se anula
   * sólo la plata; el stock que entró se corrige a mano si corresponde.
   */
  @PostMapping("/{id}/void")
  public ResponseEntity<CashMovementResponse> voidMovement(
      @PathVariable java.util.UUID id,
      @RequestBody VoidCashMovementRequest req) {
    return ResponseEntity.ok(
        CashMovementResponse.from(voidService.voidMovement(id, req.reason())));
  }
}