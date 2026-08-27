package com.jowi.stock.procedure.repositories;

import com.jowi.stock.procedure.entities.ProcedureConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProcedureConsumptionRepository
    extends JpaRepository<ProcedureConsumption, UUID> {

  List<ProcedureConsumption> findByProcedureCode(String procedureCode);

  boolean existsByProcedureCode(String procedureCode);
}
