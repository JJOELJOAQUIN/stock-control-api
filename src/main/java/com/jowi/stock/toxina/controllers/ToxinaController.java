package com.jowi.stock.toxina.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.toxina.dto.CreateToxinaTreatmentRequest;
import com.jowi.stock.toxina.dto.RegisterToxinaSessionRequest;
import com.jowi.stock.toxina.dto.ToxinaSessionResponse;
import com.jowi.stock.toxina.services.ToxinaService;
import com.jowi.stock.treatment.dto.TreatmentResponse;

/**
 * Toxina (Xeomin). El alta de tratamiento y el pago van por acá y por
 * /api/treatments (pagos). Las sesiones y el vial son propios de este flujo.
 */
@RestController
@RequestMapping("/api/toxina")
public class ToxinaController {

  private final ToxinaService service;

  public ToxinaController(ToxinaService service) {
    this.service = service;
  }

  @PostMapping("/treatments")
  public ResponseEntity<TreatmentResponse> createTreatment(
      @RequestBody CreateToxinaTreatmentRequest req) {
    var t = service.createTreatment(req.patientId(), req.totalAmount(), req.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(TreatmentResponse.from(t));
  }

  @PostMapping("/treatments/{treatmentId}/sessions")
  public ResponseEntity<ToxinaSessionResponse> registerSession(
      @PathVariable UUID treatmentId,
      @RequestBody RegisterToxinaSessionRequest req) {
    var session = service.registerSession(
        treatmentId,
        req.productId(),
        req.sessionNumber() == null ? 1 : req.sessionNumber(),
        req.unitsUsed() == null ? 0 : req.unitsUsed(),
        req.context());
    return ResponseEntity.status(HttpStatus.CREATED).body(ToxinaSessionResponse.from(session));
  }

  @GetMapping("/treatments/{treatmentId}/sessions")
  public ResponseEntity<List<ToxinaSessionResponse>> sessions(@PathVariable UUID treatmentId) {
    var list = service.getSessions(treatmentId).stream()
        .map(ToxinaSessionResponse::from).toList();
    return ResponseEntity.ok(list);
  }
}