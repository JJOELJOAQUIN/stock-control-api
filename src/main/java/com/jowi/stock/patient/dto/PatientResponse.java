package com.jowi.stock.patient.dto;

import java.time.Instant;
import java.util.UUID;

import com.jowi.stock.patient.entities.Patient;

public record PatientResponse(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    String dni,
    String email,
    String observations,
    Boolean active,
    Instant createdAt) {

  public static PatientResponse from(Patient p) {
    return new PatientResponse(
        p.getId(),
        p.getFirstName(),
        p.getLastName(),
        p.getPhone(),
        p.getDni(),
        p.getEmail(),
        p.getObservations(),
        p.getActive(),
        p.getCreatedAt());
  }
}