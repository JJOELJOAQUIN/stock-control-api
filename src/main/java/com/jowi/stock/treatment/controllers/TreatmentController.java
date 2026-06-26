package com.jowi.stock.treatment.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.treatment.dto.CreateTreatmentRequest;
import com.jowi.stock.treatment.dto.RegisterPaymentRequest;
import com.jowi.stock.treatment.dto.TreatmentPaymentResponse;
import com.jowi.stock.treatment.dto.TreatmentResponse;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.entities.TreatmentPayment;
import com.jowi.stock.treatment.services.TreatmentService;

@RestController
@RequestMapping("/api/treatments")
public class TreatmentController {

  private final TreatmentService service;

  public TreatmentController(TreatmentService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<TreatmentResponse> create(
      @Valid @RequestBody CreateTreatmentRequest req) {
    Treatment created = service.createTreatment(
        req.procedureCode(),
        req.procedureLabel(),
        req.patientId(),
        req.context(),
        req.totalAmount(),
        req.cosmetologistFixedShare(),
        req.comment(),
        req.firstPaymentAmount(),
        req.firstPaymentMethod());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(TreatmentResponse.from(created, service.resolveStatus(created)));
  }

  @PostMapping("/{id}/payments")
  public ResponseEntity<TreatmentPaymentResponse> registerPayment(
      @PathVariable UUID id,
      @Valid @RequestBody RegisterPaymentRequest req) {
    TreatmentPayment payment = service.registerPayment(
        id, req.amount(), req.paymentMethod(), req.comment());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(TreatmentPaymentResponse.from(payment));
  }

  @GetMapping
  public ResponseEntity<List<TreatmentResponse>> list(
      @RequestParam CashContext context,
      @RequestParam(required = false, defaultValue = "false") boolean pendingOnly) {
    List<Treatment> treatments = pendingOnly
        ? service.listPendingByContext(context)
        : service.listByContext(context);

    return ResponseEntity.ok(toResponses(treatments));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TreatmentResponse> getById(@PathVariable UUID id) {
    Treatment t = service.getById(id);
    return ResponseEntity.ok(TreatmentResponse.from(t, service.resolveStatus(t)));
  }

  @GetMapping("/by-patient/{patientId}")
  public ResponseEntity<List<TreatmentResponse>> listByPatient(
      @PathVariable UUID patientId) {
    return ResponseEntity.ok(toResponses(service.listByPatient(patientId)));
  }

  private List<TreatmentResponse> toResponses(List<Treatment> treatments) {
    return treatments.stream()
        .map(t -> TreatmentResponse.from(t, service.resolveStatus(t)))
        .toList();
  }
}