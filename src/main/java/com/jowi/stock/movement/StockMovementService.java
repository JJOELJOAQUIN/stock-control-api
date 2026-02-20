package com.jowi.stock.movement;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductRepository;
import com.jowi.stock.stock.StockContext;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
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

  public StockMovementService(
      StockMovementRepository repository,
      ProductRepository productRepository) {
    this.repository = repository;
    this.productRepository = productRepository;
  }

  public StockMovement register(
      UUID productId,
      StockContext context,
      StockMovementType type,
      int quantity,
      StockMovementReason reasonType,
      String comment) {

    validate(context, type, quantity, reasonType);

    Product product = productRepository.findById(productId)
        .orElseThrow(() ->
            new EntityNotFoundException("Product not found: " + productId));

    StockMovement movement = new StockMovement();
    movement.setProduct(product);
    movement.setContext(context);
    movement.setType(type);
    movement.setQuantity(quantity);
    movement.setReasonType(reasonType);
    movement.setComment(comment);

    return repository.save(movement);
  }

  public Page<StockMovement> search(
      UUID productId,
      StockContext context,
      StockMovementType type,
      StockMovementReason reason,
      Integer minQty,
      Integer maxQty,
      OffsetDateTime from,
      OffsetDateTime to,
      Pageable pageable) {

    if (productId == null) {
      throw new IllegalArgumentException("productId is required");
    }

    Specification<StockMovement> spec =
        Specification.where(StockMovementSpecification.byProduct(productId))
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
