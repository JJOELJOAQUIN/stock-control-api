package com.jowi.stock.cash.specifications;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;

/**
 * Filtros componibles para la búsqueda de movimientos de caja.
 * Cada método devuelve null cuando el filtro no aplica, de modo que
 * al encadenarlos con {@code .and()} el filtro simplemente se ignora.
 */
public final class CashMovementSpecifications {

  private CashMovementSpecifications() {
  }

  public static Specification<CashMovement> hasContext(CashContext context) {
    return (root, query, cb) -> context == null
        ? null
        : cb.equal(root.get("context"), context);
  }

  public static Specification<CashMovement> hasType(CashMovementType type) {
    return (root, query, cb) -> type == null
        ? null
        : cb.equal(root.get("type"), type);
  }

  public static Specification<CashMovement> hasSource(CashSource source) {
    return (root, query, cb) -> source == null
        ? null
        : cb.equal(root.get("source"), source);
  }

  public static Specification<CashMovement> createdFrom(Instant from) {
    return (root, query, cb) -> from == null
        ? null
        : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<CashMovement> createdBefore(Instant to) {
    return (root, query, cb) -> to == null
        ? null
        : cb.lessThan(root.get("createdAt"), to);
  }

  /** Busca el texto (case-insensitive) tanto en comentario como en detalle. */
  public static Specification<CashMovement> textContains(String q) {
    return (root, query, cb) -> {
      if (q == null || q.isBlank()) {
        return null;
      }
      String like = "%" + q.trim().toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(cb.coalesce(root.get("comment"), "")), like),
          cb.like(cb.lower(cb.coalesce(root.get("detail"), "")), like));
    };
  }
}