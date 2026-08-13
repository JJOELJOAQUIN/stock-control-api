package com.jowi.stock.treatment.repositories;

import com.jowi.stock.treatment.entities.Treatment;
import com.jowi.stock.treatment.enums.TreatmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {
  List<Treatment> findByPatientId(UUID patientId);
  List<Treatment> findByStatusIn(List<TreatmentStatus> statuses);
  List<Treatment> findAllByOrderByStatusAscCreatedAtDesc();
  List<Treatment> findByCodeOrderByCreatedAtDesc(String code);
}