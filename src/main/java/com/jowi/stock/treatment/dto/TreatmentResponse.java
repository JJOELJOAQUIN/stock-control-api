package com.jowi.stock.treatment.dto;

import com.jowi.stock.treatment.entities.Treatment;
import java.math.BigDecimal;
import java.util.UUID;

public record TreatmentResponse(
    UUID id,
    UUID patientId,
    String patientName,
    String code,
    String description,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal remainingAmount,
    BigDecimal cosmetologistFixedShare,
    Integer maxInstallments,
    Integer paymentsCount,
    String status) {

  public static TreatmentResponse from(Treatment t) {
    return new TreatmentResponse(
        t.getId(),
        t.getPatient().getId(),
        t.getPatient().getFirstName() + " " + t.getPatient().getLastName(),
        t.getCode(),
        t.getDescription(),
        t.getTotalAmount(),
        t.getPaidAmount(),
        t.getTotalAmount().subtract(t.getPaidAmount()),
        t.getCosmetologistFixedShare(),
        t.getMaxInstallments(),
        t.getPayments() == null ? 0 : t.getPayments().size(),
        t.getStatus().name());
  }
}
