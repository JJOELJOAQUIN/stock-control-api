package com.jowi.stock.cash.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.batch.dto.BatchAllocation;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.repositories.CashMovementRepository;
import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.entities.StockMovementBatch;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.services.StockMovementService;
import com.jowi.stock.stock.services.StockService;
import com.jowi.stock.treatment.services.TreatmentService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

/**
 * Anulación de movimientos de caja: soft delete visible.
 *
 * El movimiento queda en la tabla, marcado (voided, fecha, motivo, quién), y
 * desaparece de todas las agregaciones — que filtran voided = false en el
 * repositorio. Además revierte lo que el movimiento arrastró:
 *
 *  - Venta o sesión con insumos: devuelve el stock, y cada unidad vuelve al
 *    lote del que salió (stock_movement_batches). Para salidas anteriores a
 *    la trazabilidad de lotes, devolución aproximada al lote de vencimiento
 *    más tardío.
 *  - Pago de tratamiento: baja paid_amount, recalcula el estado y elimina la
 *    fila de treatment_payments para que la próxima cuota se numere bien.
 *  - Compra (PROVIDER_PAYMENT): anula SOLO la plata. El movimiento de una
 *    compra no registra qué productos entraron, así que revertir su stock
 *    sería adivinar; el ajuste va a mano si corresponde.
 *
 * Todo corre en una transacción: si la reversión de stock falla, el
 * movimiento no queda anulado a medias.
 */
@Service
@Transactional
public class CashVoidService {

  private final CashMovementRepository cashRepository;
  private final StockMovementService stockMovementService;
  private final StockService stockService;
  private final ProductBatchService batchService;
  private final TreatmentService treatmentService;
  private final CurrentUserService currentUserService;

  public CashVoidService(
      CashMovementRepository cashRepository,
      StockMovementService stockMovementService,
      StockService stockService,
      ProductBatchService batchService,
      TreatmentService treatmentService,
      CurrentUserService currentUserService) {
    this.cashRepository = cashRepository;
    this.stockMovementService = stockMovementService;
    this.stockService = stockService;
    this.batchService = batchService;
    this.treatmentService = treatmentService;
    this.currentUserService = currentUserService;
  }

  public CashMovement voidMovement(UUID cashMovementId, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("El motivo de la anulación es obligatorio");
    }

    CashMovement movement = cashRepository.findById(cashMovementId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Cash movement not found: " + cashMovementId));

    if (movement.isVoided()) {
      throw new IllegalStateException("El movimiento ya está anulado");
    }

    // 1) Revertir lo que el movimiento arrastró. El stock se decide por los
    // stock_movements linkeados y no por el source: así cubre ventas simples,
    // combinadas y sesiones con insumos con la misma lógica, sin enumerar
    // casos que se van a olvidar cuando aparezca un flujo nuevo.
    restoreStockFor(movement);

    if (movement.getSource() == CashSource.PROCEDURE) {
      treatmentService.revertPaymentByCashMovement(movement.getId());
    }

    // 2) Marcar. Los montos no se tocan: la historia queda tal cual se
    // registró, sólo que fuera de las sumas.
    movement.setVoided(true);
    movement.setVoidedAt(Instant.now());
    movement.setVoidReason(reason.trim());
    movement.setVoidedBy(currentUserService.currentUserLabel());

    return cashRepository.save(movement);
  }

  /**
   * Devuelve el stock de las salidas linkeadas al movimiento, lote por lote.
   *
   * Las unidades con lote asignado vuelven con restore() al lote exacto; las
   * que no lo tienen (salidas anteriores a la trazabilidad) vuelven con
   * restoreApproximate(). El reingreso queda registrado como stock_movement
   * IN con motivo ANULACION y las mismas allocations, así el kardex cuenta la
   * historia completa: salió por venta, volvió por anulación.
   */
  private void restoreStockFor(CashMovement movement) {
    List<StockMovement> stockMovements =
        stockMovementService.findByCashMovement(movement.getId());

    for (StockMovement sm : stockMovements) {
      if (sm.getType() != StockMovementType.OUT) {
        continue;
      }

      List<StockMovementBatch> allocations =
          stockMovementService.findAllocations(sm.getId());

      int allocated = 0;
      for (StockMovementBatch allocation : allocations) {
        batchService.restore(allocation.getBatch().getId(), allocation.getQuantity());
        allocated += allocation.getQuantity();
      }

      int withoutBatch = sm.getQuantity() - allocated;
      if (withoutBatch > 0) {
        batchService.restoreApproximate(
            sm.getProduct().getId(), sm.getContext(), withoutBatch);
      }

      stockService.increase(
          sm.getProduct().getId(),
          sm.getContext(),
          sm.getQuantity(),
          StockMovementReason.ANULACION,
          "Anulación - " + movement.getId(),
          allocations.stream()
              .map(a -> new BatchAllocation(a.getBatch().getId(), a.getQuantity()))
              .toList());
    }
  }
} 