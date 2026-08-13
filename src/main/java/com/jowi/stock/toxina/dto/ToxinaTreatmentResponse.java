package com.jowi.stock.toxina.dto;

import com.jowi.stock.treatment.entities.Treatment;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Un tratamiento de toxina con sus sesiones, para la tabla de la sección de
 * toxina (GET /api/toxina/treatments). El estado y lo pagado salen del propio
 * Treatment; las sesiones traen unidades, fecha y datos del vial.
 */
public record ToxinaTreatmentResponse(
    UUID id,
    UUID patientId,
    String patientName,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    String status,
    List<ToxinaSessionResponse> sessions) {

  public static ToxinaTreatmentResponse from(Treatment t, List<ToxinaSessionResponse> sessions) {
    String name = t.getPatient() == null
        ? ""
        : ((t.getPatient().getFirstName() == null ? "" : t.getPatient().getFirstName())
            + " "
            + (t.getPatient().getLastName() == null ? "" : t.getPatient().getLastName())).trim();
    return new ToxinaTreatmentResponse(
        t.getId(),
        t.getPatient() == null ? null : t.getPatient().getId(),
        name,
        t.getTotalAmount(),
        t.getPaidAmount(),
        t.getStatus() == null ? null : t.getStatus().name(),
        sessions);
  }
}