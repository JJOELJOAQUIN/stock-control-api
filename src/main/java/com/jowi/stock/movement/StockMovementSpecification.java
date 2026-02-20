package com.jowi.stock.movement;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.jowi.stock.stock.StockContext;

public class StockMovementSpecification {

  public static Specification<StockMovement> byProduct(UUID productId) {
    return (root, query, cb) ->
        cb.equal(root.get("product").get("id"), productId);
  }

  
  public static Specification<StockMovement> byType(StockMovementType type) {
    if (type == null) return null;
    return (root, query, cb) ->
        cb.equal(root.get("type"), type);
  }

  public static Specification<StockMovement> byReason(StockMovementReason reason) {
    if (reason == null) return null;
    return (root, query, cb) ->
        cb.equal(root.get("reasonType"), reason);
  }

  public static Specification<StockMovement> quantityGte(Integer minQty) {
    if (minQty == null) return null;
    return (root, query, cb) ->
        cb.greaterThanOrEqualTo(root.get("quantity"), minQty);
  }

  public static Specification<StockMovement> quantityLte(Integer maxQty) {
    if (maxQty == null) return null;
    return (root, query, cb) ->
        cb.lessThanOrEqualTo(root.get("quantity"), maxQty);
  }

  public static Specification<StockMovement> fromDate(OffsetDateTime from) {
    if (from == null) return null;
    return (root, query, cb) ->
        cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<StockMovement> byContext(StockContext context) {
  if (context == null) {
    return null;
  }

  return (root, query, cb) ->
      cb.equal(root.get("context"), context);
}


  public static Specification<StockMovement> toDate(OffsetDateTime to) {
    if (to == null) return null;
    return (root, query, cb) ->
        cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }
}
