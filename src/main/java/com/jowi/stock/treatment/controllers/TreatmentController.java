package com.jowi.stock.treatment.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.patient.dto.CreatePatientRequest;
import com.jowi.stock.patient.dto.PatientResponse;
import com.jowi.stock.treatment.dto.*;
import com.jowi.stock.treatment.enums.TreatmentStatus;
import com.jowi.stock.treatment.services.TreatmentService;

@RestController
@RequestMapping("/api/treatments")
public class TreatmentController {

    private final TreatmentService service;

    public TreatmentController(TreatmentService service) {
        this.service = service;
    }

    // ---- Pacientes ----

    @PostMapping("/patients")
    public ResponseEntity<PatientResponse> createPatient(@RequestBody CreatePatientRequest req) {
        var p = service.createPatient(req.firstName(), req.lastName(), req.dni(), req.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponse.from(p));
    }

    @GetMapping("/patients")
    public ResponseEntity<List<PatientResponse>> searchPatients(
            @RequestParam(required = false) String q) {
        var list = service.searchPatients(q).stream().map(PatientResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    // ---- Tratamientos ----

    @PostMapping
    public ResponseEntity<TreatmentResponse> createTreatment(
            @RequestBody CreateTreatmentRequest req) {
        var t = service.createTreatment(
                req.patientId(),
                req.code(),
                req.description(),
                req.totalAmount(),
                req.cosmetologistFixedShare(),
                req.maxInstallments() == null ? 2 : req.maxInstallments());
        return ResponseEntity.status(HttpStatus.CREATED).body(TreatmentResponse.from(t));
    }

    @GetMapping("/{treatmentId}")
    public ResponseEntity<TreatmentResponse> getTreatment(@PathVariable UUID treatmentId) {
        return ResponseEntity.ok(TreatmentResponse.from(service.getTreatment(treatmentId)));
    }

    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<List<TreatmentResponse>> byPatient(@PathVariable UUID patientId) {
        var list = service.getPatientTreatments(patientId).stream()
                .map(TreatmentResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{treatmentId}/payments")
    public ResponseEntity<List<PaymentResponse>> payments(@PathVariable UUID treatmentId) {
        var list = service.getTreatmentPayments(treatmentId).stream()
                .map(PaymentResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    // ---- Pagos ----

    @PostMapping("/{treatmentId}/payments")
    public ResponseEntity<PaymentResponse> registerPayment(
            @PathVariable UUID treatmentId,
            @RequestBody RegisterPaymentRequest req) {
        var payment = service.registerPayment(
                treatmentId, req.amount(), req.paymentMethod(), req.context(), req.splitPreset());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping
    public ResponseEntity<List<TreatmentResponse>> listTreatments(
            @RequestParam(required = false) TreatmentStatus status) {
        var list = service.listTreatments(status).stream()
                .map(TreatmentResponse::from).toList();
        return ResponseEntity.ok(list);
    }
}