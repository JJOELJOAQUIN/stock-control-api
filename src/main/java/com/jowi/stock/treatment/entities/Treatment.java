package com.jowi.stock.treatment.entities;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.patient.entities.Patient;
import com.jowi.stock.treatment.enums.TreatmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "treatments", indexes = {
    @Index(name = "idx_treatment_patient", columnList = "patient_id"),
    @Index(name = "idx_treatment_status", columnList = "status")
})
public class Treatment extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  private Patient patient;

  // Código del protocolo (ej. el del peeling). Genérico para otros a futuro.
  @NotNull
  @Column(nullable = false, length = 60)
  private String code;

  @Column(length = 200)
  private String description;

  @NotNull
  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @NotNull
  @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO;

  // Monto fijo que le corresponde a la cosmetóloga (se aplica en el 1er pago).
  @Column(name = "cosmetologist_fixed_share", precision = 18, scale = 2)
  private BigDecimal cosmetologistFixedShare;

  // Tope de cuotas (2 para peeling). Genérico.
  @NotNull
  @Column(name = "max_installments", nullable = false)
  private Integer maxInstallments;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TreatmentStatus status = TreatmentStatus.PENDIENTE;

  @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Payment> payments = new ArrayList<>();

  public void addPayment(Payment p) {
    p.setTreatment(this);
    this.payments.add(p);
  }

  public Patient getPatient() { return patient; }
  public void setPatient(Patient patient) { this.patient = patient; }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

  public BigDecimal getPaidAmount() { return paidAmount; }
  public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

  public BigDecimal getCosmetologistFixedShare() { return cosmetologistFixedShare; }
  public void setCosmetologistFixedShare(BigDecimal v) { this.cosmetologistFixedShare = v; }

  public Integer getMaxInstallments() { return maxInstallments; }
  public void setMaxInstallments(Integer maxInstallments) { this.maxInstallments = maxInstallments; }

  public TreatmentStatus getStatus() { return status; }
  public void setStatus(TreatmentStatus status) { this.status = status; }

  public List<Payment> getPayments() { return payments; }
  public void setPayments(List<Payment> payments) { this.payments = payments; }
}