package com.jowi.stock.treatment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTreatmentRequest(
    UUID patientId,
    String code,
    String description,
    BigDecimal totalAmount,
    BigDecimal cosmetologistFixedShare,
    Integer maxInstallments) {
}
