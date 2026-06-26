package com.jowi.stock.treatment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.patient.dto.PatientResponse;
import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.enums.TreatmentStatus;

public record TreatmentResponse(
    UUID id,
    String procedureCode,
    String procedureLabel,
    PatientResponse patient,
    CashContext context,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal pendingAmount,
    TreatmentStatus status,
    BigDecimal cosmetologistFixedShare,
    String comment,
    List<TreatmentPaymentResponse> payments,
    Instant createdAt) {

  public static TreatmentResponse from(Treatment t, TreatmentStatus status) {
    return new TreatmentResponse(
        t.getId(),
        t.getProcedureCode(),
        t.getProcedureLabel(),
        t.getPatient() != null ? PatientResponse.from(t.getPatient()) : null,
        t.getContext(),
        t.getTotalAmount(),
        t.getPaidAmount(),
        t.getPendingAmount(),
        status,
        t.getCosmetologistFixedShare(),
        t.getComment(),
        t.getPayments().stream()
            .map(TreatmentPaymentResponse::from)
            .toList(),
        t.getCreatedAt());
  }
}