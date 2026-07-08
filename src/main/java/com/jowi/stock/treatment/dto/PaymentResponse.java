package com.jowi.stock.treatment.dto;

import com.jowi.stock.treatment.entities.Payment;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    BigDecimal amount,
    String paymentMethod,
    Integer installmentNumber,
    UUID cashMovementId) {

  public static PaymentResponse from(Payment p) {
    return new PaymentResponse(
        p.getId(),
        p.getAmount(),
        p.getPaymentMethod().name(),
        p.getInstallmentNumber(),
        p.getCashMovementId());
  }
}