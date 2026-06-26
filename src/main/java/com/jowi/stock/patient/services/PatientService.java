package com.jowi.stock.patient.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jowi.stock.patient.dto.CreatePatientRequest;
import com.jowi.stock.patient.entities.Patient;
import com.jowi.stock.patient.repositories.PatientRepository;

@Service
@Transactional
public class PatientService {

  private final PatientRepository repository;

  public PatientService(PatientRepository repository) {
    this.repository = repository;
  }

  public Patient create(CreatePatientRequest req) {
    if (req == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (req.firstName() == null || req.firstName().isBlank()) {
      throw new IllegalArgumentException("firstName is required");
    }
    if (req.lastName() == null || req.lastName().isBlank()) {
      throw new IllegalArgumentException("lastName is required");
    }

    Patient p = new Patient();
    p.setFirstName(req.firstName().trim());
    p.setLastName(req.lastName().trim());
    p.setPhone(blankToNull(req.phone()));
    p.setDni(blankToNull(req.dni()));
    p.setEmail(blankToNull(req.email()));
    p.setObservations(blankToNull(req.observations()));
    p.setActive(true);

    return repository.save(p);
  }

  @Transactional(readOnly = true)
  public List<Patient> search(String term) {
    if (term == null || term.isBlank()) {
      return repository.findAll();
    }
    return repository.search(term.trim());
  }

  @Transactional(readOnly = true)
  public Patient getById(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("patient not found"));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}