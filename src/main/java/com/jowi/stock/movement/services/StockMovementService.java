package com.jowi.stock.movement.services;

import com.jowi.stock.batch.dto.BatchAllocation;
import com.jowi.stock.batch.repositories.ProductBatchRepository;
import com.jowi.stock.movement.entities.StockMovement;
import com.jowi.stock.movement.entities.StockMovementBatch;
import com.jowi.stock.movement.entities.StockMovementSpecification;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.repositories.StockMovementBatchRepository;
import com.jowi.stock.movement.repositories.StockMovementRepository;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.repositories.ProductRepository;
import com.jowi.stock.stock.enums.StockContext;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StockMovementService {

  private final StockMovementRepository repository;
  private final ProductRepository productRepository;
  private final StockMovementBatchRepository movementBatchRepository;
  private final ProductBatchRepository batchRepository;

  public StockMovementService(
      StockMovementRepository repository,
      ProductRepository productRepository,
      StockMovementBatchRepository movementBatchRepository,
      ProductBatchRepository batchRepository) {
    this.repository = repository;
    this.productRepository = productRepository;
    this.movementBatchRepository = movementBatchRepository;
    this.batchRepository = batchRepository;
  }

  /** Registro simple, sin trazabilidad de lotes ni caja. */
  public StockMovement register(
      UUID productId,
      StockContext context,
      StockMovementType type,
      int quantity,
      StockMovementReason reasonType,
      String comment) {

    return register(productId, context, type, quantity, reasonType, comment, List.of());
  }

  /**
   * Registro con trazabilidad de lotes: además del movimiento, deja una fila
   * por cada lote que participó, con cuántas unidades salieron de cada uno.
   */
  public StockMovement register(
      UUID productId,
      StockContext context,
      StockMovementType type,
      int quantity,
      StockMovementReason reasonType,
      String comment,
      List<BatchAllocation> allocations) {

    validate(context, type, quantity, reasonType);

    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

    StockMovement movement = new StockMovement();
    movement.setProduct(product);
    movement.setContext(context);
    movement.setType(type);
    movement.setQuantity(quantity);
    movement.setReasonType(reasonType);
    movement.setComment(comment);

    StockMovement saved = repository.save(movement);

    if (allocations != null && !allocations.isEmpty()) {
      List<StockMovementBatch> rows = allocations.stream()
          .filter(a -> a.quantity() > 0)
          .map(a -> {
            StockMovementBatch row = new StockMovementBatch();
            row.setStockMovement(saved);
            row.setBatch(batchRepository.getReferenceById(a.batchId()));
            row.setQuantity(a.quantity());
            return row;
          })
          .toList();

      movementBatchRepository.saveAll(rows);
    }

    return saved;
  }

  /**
   * Ata un movimiento de stock ya registrado a su movimiento de caja.
   *
   * Se hace en dos pasos y no en el alta porque el stock se descuenta ANTES
   * de crear el movimiento de caja (si el stock no alcanza, no queremos
   * haber tocado la caja). Recién cuando la caja existe sabemos su id.
   */
  public void linkToCashMovement(UUID stockMovementId, UUID cashMovementId) {
    if (stockMovementId == null || cashMovementId == null) {
      return;
    }

    StockMovement movement = repository.findById(stockMovementId)
        .orElseThrow(() -> new EntityNotFoundException(
            "StockMovement not found: " + stockMovementId));

    movement.setCashMovementId(cashMovementId);
    repository.save(movement);
  }

  /** Versión batch de {@link #linkToCashMovement}, para ventas combinadas. */
  public void linkToCashMovement(List<UUID> stockMovementIds, UUID cashMovementId) {
    if (stockMovementIds == null || stockMovementIds.isEmpty() || cashMovementId == null) {
      return;
    }

    List<StockMovement> movements = repository.findAllById(stockMovementIds);
    movements.forEach(m -> m.setCashMovementId(cashMovementId));
    repository.saveAll(movements);
  }

  @Transactional
  public List<StockMovement> findByCashMovement(UUID cashMovementId) {
    return repository.findByCashMovementIdOrderByCreatedAtAsc(cashMovementId);
  }

  @Transactional
  public List<StockMovementBatch> findAllocations(UUID stockMovementId) {
    return movementBatchRepository.findByStockMovement_Id(stockMovementId);
  }

  public Page<StockMovement> search(
      UUID productId,
      StockContext context,
      StockMovementType type,
      StockMovementReason reason,
      Integer minQty,
      Integer maxQty,
      java.time.LocalDateTime from,
      java.time.LocalDateTime to,
      Pageable pageable) {

    if (productId == null) {
      throw new IllegalArgumentException("productId is required");
    }

    Specification<StockMovement> spec = Specification.where(StockMovementSpecification.byProduct(productId))
        .and(StockMovementSpecification.byContext(context))
        .and(StockMovementSpecification.byType(type))
        .and(StockMovementSpecification.byReason(reason))
        .and(StockMovementSpecification.quantityGte(minQty))
        .and(StockMovementSpecification.quantityLte(maxQty))
        .and(StockMovementSpecification.fromDate(from))
        .and(StockMovementSpecification.toDate(to));

    return repository.findAll(spec, pageable);
  }

  private void validate(
      StockContext context,
      StockMovementType type,
      int quantity,
      StockMovementReason reasonType) {

    if (context == null) {
      throw new IllegalArgumentException("Context is required");
    }
    if (type == null) {
      throw new IllegalArgumentException("Movement type is required");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    if (reasonType == null) {
      throw new IllegalArgumentException("Movement reasonType is required");
    }
  }


}
