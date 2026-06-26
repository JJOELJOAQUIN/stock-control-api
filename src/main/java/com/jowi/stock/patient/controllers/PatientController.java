package com.jowi.stock.patient.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.patient.dto.CreatePatientRequest;
import com.jowi.stock.patient.dto.PatientResponse;
import com.jowi.stock.patient.entities.Patient;
import com.jowi.stock.patient.services.PatientService;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

  private final PatientService service;

  public PatientController(PatientService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<PatientResponse> create(
      @Valid @RequestBody CreatePatientRequest req) {
    Patient created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(PatientResponse.from(created));
  }

  @GetMapping
  public ResponseEntity<List<PatientResponse>> search(
      @RequestParam(required = false) String term) {
    List<PatientResponse> results = service.search(term).stream()
        .map(PatientResponse::from)
        .toList();
    return ResponseEntity.ok(results);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PatientResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(PatientResponse.from(service.getById(id)));
  }
}