package com.jowi.stock.toxina.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Alta de un tratamiento de toxina para un paciente. El precio es editable
 * (hoy 150.000, pago único). description es opcional.
 */
public record CreateToxinaTreatmentRequest(
    UUID patientId,
    BigDecimal totalAmount,
    String description
) {}