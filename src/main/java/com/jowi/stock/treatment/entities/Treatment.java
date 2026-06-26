package com.jowi.stock.treatment.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.patient.entities.Patient;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Tratamiento registrado (instancia). No confundir con el catálogo de
 * procedimientos del front: esto es "a tal paciente se le hizo tal
 * procedimiento, cuesta tanto, y se paga en uno o varios pagos".
 *
 * El saldo y el estado se calculan a partir de los pagos; no se persisten
 * como columnas para evitar desincronización.
 */
@Entity
@Table(name = "treatments", indexes = {
    @Index(name = "idx_treatment_patient", columnList = "patient_id"),
    @Index(name = "idx_treatment_context", columnList = "context")
})
public class Treatment extends BaseEntity {

  // Identificación del procedimiento (del catálogo del front).
  @NotNull
  @Column(name = "procedure_code", nullable = false, length = 80)
  private String procedureCode;

  @Column(name = "procedure_label", length = 160)
  private String procedureLabel;

  // Paciente: opcional a nivel de modelo; la obligatoriedad para peeling
  // se valida en el service.
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id")
  private Patient patient;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashContext context;

  @NotNull
  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  // Reparto fijo de la cosmetóloga para este tratamiento (ej: peeling $40.000).
  // Si es null, el reparto sigue las reglas por defecto del procedimiento.
  @Column(name = "cosmetologist_fixed_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistFixedShare;

  @Column(length = 500)
  private String comment;

  @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TreatmentPayment> payments = new ArrayList<>();

  // ===== getters/setters =====

  public String getProcedureCode() {
    return procedureCode;
  }

  public void setProcedureCode(String procedureCode) {
    this.procedureCode = procedureCode;
  }

  public String getProcedureLabel() {
    return procedureLabel;
  }

  public void setProcedureLabel(String procedureLabel) {
    this.procedureLabel = procedureLabel;
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public CashContext getContext() {
    return context;
  }

  public void setContext(CashContext context) {
    this.context = context;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public BigDecimal getCosmetologistFixedShare() {
    return cosmetologistFixedShare;
  }

  public void setCosmetologistFixedShare(BigDecimal cosmetologistFixedShare) {
    this.cosmetologistFixedShare = cosmetologistFixedShare;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<TreatmentPayment> getPayments() {
    return payments;
  }

  public void setPayments(List<TreatmentPayment> payments) {
    this.payments = payments;
  }

  // ===== lógica derivada (no persistida) =====

  /** Suma de todos los pagos registrados. */
  public BigDecimal getPaidAmount() {
    return payments.stream()
        .map(TreatmentPayment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Saldo pendiente = total - pagado (nunca negativo). */
  public BigDecimal getPendingAmount() {
    BigDecimal pending = totalAmount.subtract(getPaidAmount());
    return pending.signum() < 0 ? BigDecimal.ZERO : pending;
  }
}