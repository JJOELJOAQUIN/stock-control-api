package com.jowi.stock.patient.dto;

import com.jowi.stock.patient.entities.Patient;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    String firstName,
    String lastName,
    String dni,
    String phone) {

  public static PatientResponse from(Patient p) {
    return new PatientResponse(
        p.getId(),
        p.getFirstName(),
        p.getLastName(),
        p.getDni(),
        p.getPhone());
  }
}