package com.jowi.stock.patient.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jowi.stock.patient.entities.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

  @Query("""
        SELECT p FROM Patient p
        WHERE p.active = true
          AND (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
            OR p.dni LIKE CONCAT('%', :term, '%')
          )
        ORDER BY p.lastName ASC, p.firstName ASC
      """)
  List<Patient> search(@Param("term") String term);
}