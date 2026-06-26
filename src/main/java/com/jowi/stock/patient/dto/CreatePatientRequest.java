package com.jowi.stock.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePatientRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phone,
    String dni,
    String email,
    String observations) {
}