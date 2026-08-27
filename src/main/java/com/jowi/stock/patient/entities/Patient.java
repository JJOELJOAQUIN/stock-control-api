package com.jowi.stock.patient.entities;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_dni", columnList = "dni"),
    @Index(name = "idx_patient_lastname", columnList = "last_name")
})
public class Patient extends BaseEntity {

  @NotBlank
  @Column(name = "first_name", nullable = false, length = 120)
  private String firstName;

  @NotBlank
  @Column(name = "last_name", nullable = false, length = 120)
  private String lastName;

  // Opcional, pero único cuando está presente (Postgres permite múltiples NULL).
  @Column(unique = true, length = 30)
  private String dni;

  @Column(length = 40)
  private String phone;

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getDni() { return dni; }
  public void setDni(String dni) { this.dni = dni; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
}
