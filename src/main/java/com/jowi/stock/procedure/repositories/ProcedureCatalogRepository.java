package com.jowi.stock.procedure.repositories;

import com.jowi.stock.procedure.entities.ProcedureCatalog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedureCatalogRepository
    extends JpaRepository<ProcedureCatalog, UUID> {

  /** Sólo activos: es lo que consultan los resolvers de reparto en caja. */
  Optional<ProcedureCatalog> findByCodeIgnoreCaseAndActiveTrue(String code);

  Optional<ProcedureCatalog> findByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCase(String code);

  List<ProcedureCatalog> findAllByOrderByKindAscLabelAsc();

  List<ProcedureCatalog> findByActiveTrueOrderByKindAscLabelAsc();
}
