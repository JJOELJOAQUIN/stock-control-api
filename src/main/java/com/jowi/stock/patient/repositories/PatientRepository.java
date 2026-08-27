package com.jowi.stock.patient.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import com.jowi.stock.patient.entities.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
  Optional<Patient> findByDni(String dni);
  List<Patient> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
      String lastName, String firstName);
}
