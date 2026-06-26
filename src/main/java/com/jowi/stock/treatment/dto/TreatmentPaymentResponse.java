package com.jowi.stock.treatment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.treatment.entities.TreatmentPayment;

public record TreatmentPaymentResponse(
    UUID id,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    UUID cashMovementId,
    BigDecimal doctorShare,
    BigDecimal cosmetologistShare,
    String comment,
    Instant createdAt) {

  public static TreatmentPaymentResponse from(TreatmentPayment p) {
    return new TreatmentPaymentResponse(
        p.getId(),
        p.getAmount(),
        p.getPaymentMethod(),
        p.getCashMovementId(),
        p.getDoctorShare(),
        p.getCosmetologistShare(),
        p.getComment(),
        p.getCreatedAt());
  }
}