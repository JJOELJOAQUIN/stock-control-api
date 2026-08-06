package com.jowi.stock.toxina.repositories;

import com.jowi.stock.toxina.entities.OpenVial;
import com.jowi.stock.toxina.enums.OpenVialStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OpenVialRepository extends JpaRepository<OpenVial, UUID> {

  // Todos los viales en un estado, el más viejo primero (para el aviso).
  List<OpenVial> findByStatusOrderByOpenedAtAsc(OpenVialStatus status);

  // Viales de un producto en un estado, el más viejo primero. Se usa al
  // registrar una sesión: se reusa el vial abierto más próximo a vencer.
  List<OpenVial> findByProductIdAndStatusOrderByOpenedAtAsc(UUID productId, OpenVialStatus status);
}