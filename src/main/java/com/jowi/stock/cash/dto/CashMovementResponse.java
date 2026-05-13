package com.jowi.stock.cash.dto;

import java.time.Instant;
import java.util.UUID;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;

import java.math.BigDecimal;

public record CashMovementResponse(
    UUID id,
    CashMovementType type,
    CashSource source,
    PaymentMethod paymentMethod,
    CashContext context,
    BigDecimal amount,
    BigDecimal retention,
    BigDecimal netAmount,
    BigDecimal doctorShare,
    BigDecimal cosmetologistShare,
    String comment,
    UUID referenceId,
    Instant createdAt) {
  public static CashMovementResponse from(CashMovement m) {
    return new CashMovementResponse(
        m.getId(),
        m.getType(),
        m.getSource(),
        m.getPaymentMethod(),
        m.getContext(),
        m.getAmount(),
        m.getRetention(),
        m.getNetAmount(),
        m.getDoctorShare(),
        m.getCosmetologistShare(),
        m.getComment(),
        m.getReferenceId(),
        m.getCreatedAt());
  }
}
