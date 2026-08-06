package com.jowi.stock.toxina.repositories;

import com.jowi.stock.toxina.entities.ToxinaSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ToxinaSessionRepository extends JpaRepository<ToxinaSession, UUID> {

  List<ToxinaSession> findByTreatmentIdOrderBySessionNumberAsc(UUID treatmentId);

  // Pacientes/sesiones de un vial abierto (para armar el mensaje del aviso).
  List<ToxinaSession> findByOpenVialId(UUID openVialId);
}