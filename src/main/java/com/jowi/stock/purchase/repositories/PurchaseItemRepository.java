package com.jowi.stock.purchase.repositories;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.purchase.entities.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

  /**
   * Ítems de compra del mes, con los datos de su orden (movimiento de caja).
   * El service agrupa por orden. Se excluyen las órdenes anuladas.
   *
   * Devuelve por fila:
   *   [0] cashMovementId  [1] createdAt (orden)  [2] paymentMethod
   *   [3] comment         [4] productName        [5] quantity
   *   [6] unitCost        [7] subtotal           [8] lotNumber
   *   [9] expirationDate
   */
  @Query("""
        SELECT c.id, c.createdAt, c.paymentMethod, c.comment,
               pi.productName, pi.quantity, pi.unitCost, pi.subtotal,
               pi.lotNumber, pi.expirationDate
        FROM PurchaseItem pi
        JOIN pi.cashMovement c
        WHERE c.voided = false
          AND c.context = :context
          AND c.createdAt >= :from
          AND c.createdAt < :to
        ORDER BY c.createdAt DESC, pi.productName ASC
      """)
  List<Object[]> purchasesOfMonth(
      @Param("context") CashContext context,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /**
   * Renglones de las órdenes de compra no anuladas de un contexto desde una
   * fecha, para detectar una orden cargada dos veces. Por fila:
   *   [0] cashMovementId  [1] createdAt  [2] amount  [3] productId  [4] quantity
   */
  @Query("""
        SELECT c.id, c.createdAt, c.amount, pi.productId, pi.quantity
        FROM PurchaseItem pi
        JOIN pi.cashMovement c
        WHERE c.voided = false
          AND c.context = :context
          AND c.createdAt >= :since
        """)
  List<Object[]> findOrderLinesSince(
      @Param("context") CashContext context,
      @Param("since") Instant since);

  /**
   * Fecha de la última orden no anulada con ese comentario (número de
   * pedido / proveedor), comparando sin mayúsculas ni espacios de borde.
   */
  @Query("""
        SELECT MAX(c.createdAt)
        FROM PurchaseItem pi
        JOIN pi.cashMovement c
        WHERE c.voided = false
          AND c.context = :context
          AND LOWER(TRIM(c.comment)) = :comment
        """)
  Instant findLastOrderWithComment(
      @Param("context") CashContext context,
      @Param("comment") String comment);
}
