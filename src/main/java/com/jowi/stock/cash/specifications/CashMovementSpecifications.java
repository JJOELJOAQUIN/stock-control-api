package com.jowi.stock.cash.specifications;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.jowi.stock.cash.entities.CashMovement;
import com.jowi.stock.cash.entities.CashMovementItem;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.treatment.entities.Treatment;

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

  /**
   * Busca el texto (case-insensitive) en comentario, detalle, y —clave para
   * peeling/toxina— en el nombre del PACIENTE: los pagos de tratamiento tienen
   * reference_id = treatmentId, y el Treatment tiene su Patient. Así "Sofia" o
   * "Crivelli" traen sus movimientos aunque el comentario sólo diga "Pago 1 -
   * ...".
   */
  public static Specification<CashMovement> textContains(String q) {
    return (root, query, cb) -> {
      if (q == null || q.isBlank()) {
        return null;
      }
      String like = "%" + q.trim().toLowerCase() + "%";

      var sub = query.subquery(UUID.class);
      var t = sub.from(Treatment.class);
      var patient = t.get("patient");
      sub.select(t.get("id")).where(
          cb.or(
              cb.like(cb.lower(patient.get("firstName")), like),
              cb.like(cb.lower(patient.get("lastName")), like),
              cb.like(
                  cb.lower(cb.concat(cb.concat(patient.get("firstName"), " "),
                      patient.get("lastName"))),
                  like)));

      return cb.or(
          cb.like(cb.lower(cb.coalesce(root.get("comment"), "")), like),
          cb.like(cb.lower(cb.coalesce(root.get("detail"), "")), like),
          root.get("referenceId").in(sub));
    };
  }

  /**
   * Movimientos que le corresponden a la cosmetóloga: los que tienen un ítem
   * con performed_by = COSMETOLOGA, o —para lo anterior a la trazabilidad de
   * autoría— los que le repartieron algo (cosmetologist_share > 0 en la
   * cabecera). Es el mismo criterio híbrido que usan las queries de
   * producción de su card, aplicado al listado.
   *
   * Con el ítem espejo, un peeling "Todo a Pili" entra por la primera rama
   * aunque ella cobre 0: el trabajo fue de ella y tiene que poder verlo.
   * Compras, egresos y ventas 100% de la médica quedan afuera.
   */
  public static Specification<CashMovement> visibleToCosmetologist() {
    return (root, query, cb) -> {
      var sub = query.subquery(Integer.class);
      var item = sub.from(CashMovementItem.class);
      sub.select(cb.literal(1)).where(
          cb.equal(item.get("cashMovement"), root),
          cb.equal(item.get("performedBy"), CashActor.COSMETOLOGA));

      return cb.or(
          cb.exists(sub),
          cb.greaterThan(root.get("cosmetologistShare"), BigDecimal.ZERO));
    };
  }
}
