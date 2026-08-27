package com.jowi.stock.patient.dto;

public record CreatePatientRequest(
    String firstName,
    String lastName,
    String dni,
    String phone) {
}
