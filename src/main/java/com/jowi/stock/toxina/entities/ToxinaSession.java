package com.jowi.stock.toxina.entities;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.treatment.entities.Treatment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Una sesión de toxina de un paciente. La doctora carga a mano las unidades
 * aplicadas ({@code unitsUsed}, ej. 25). Descuenta esas unidades del vial
 * abierto asociado. El tratamiento se maneja con la entidad genérica
 * {@link Treatment} (code = TOXINA_XEOMIN, pago único editable).
 */
@Entity
@Table(name = "toxina_sessions", indexes = {
    @Index(name = "idx_toxina_session_treatment", columnList = "treatment_id"),
    @Index(name = "idx_toxina_session_vial", columnList = "open_vial_id")
})
public class ToxinaSession extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "treatment_id", nullable = false)
  private Treatment treatment;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "open_vial_id", nullable = false)
  private OpenVial openVial;

  // 1 = primera sesión, 2 = segunda (~14 días después).
  @NotNull
  @Column(name = "session_number", nullable = false)
  private Integer sessionNumber;

  @NotNull
  @Column(name = "performed_at", nullable = false)
  private Instant performedAt;

  @NotNull
  @Column(name = "units_used", nullable = false)
  private Integer unitsUsed;

  // ===== getters / setters =====

  public Treatment getTreatment() { return treatment; }
  public void setTreatment(Treatment treatment) { this.treatment = treatment; }

  public OpenVial getOpenVial() { return openVial; }
  public void setOpenVial(OpenVial openVial) { this.openVial = openVial; }

  public Integer getSessionNumber() { return sessionNumber; }
  public void setSessionNumber(Integer sessionNumber) { this.sessionNumber = sessionNumber; }

  public Instant getPerformedAt() { return performedAt; }
  public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }

  public Integer getUnitsUsed() { return unitsUsed; }
  public void setUnitsUsed(Integer unitsUsed) { this.unitsUsed = unitsUsed; }
}