package com.jowi.stock.patient.entities;

import com.jowi.stock.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Paciente. Por ahora datos básicos; pensada como raíz de la futura ficha
 * clínica (antecedentes, evolución, fotos, tratamientos, pagos colgarán de acá).
 */
@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_dni", columnList = "dni"),
    @Index(name = "idx_patient_last_name", columnList = "last_name")
})
public class Patient extends BaseEntity {

  @NotBlank
  @Column(name = "first_name", nullable = false, length = 120)
  private String firstName;

  @NotBlank
  @Column(name = "last_name", nullable = false, length = 120)
  private String lastName;

  @Column(length = 40)
  private String phone;

  @Column(length = 30)
  private String dni;

  @Column(length = 160)
  private String email;

  @Column(length = 1000)
  private String observations;

  @Column(nullable = false)
  private Boolean active = true;

  // ===== getters/setters =====

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getDni() {
    return dni;
  }

  public void setDni(String dni) {
    this.dni = dni;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getObservations() {
    return observations;
  }

  public void setObservations(String observations) {
    this.observations = observations;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}