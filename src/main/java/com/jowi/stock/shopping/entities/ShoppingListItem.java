package com.jowi.stock.shopping.entities;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Ítem de la lista de compras. Es una libreta, no un pedido: la doctora anota
 * lo que necesita y lo tacha cuando lo compró.
 *
 * Las sugerencias automáticas (stock bajo, por vencer) NO se guardan acá: se
 * calculan en el front desde los productos que ya trae la pantalla. Guardar
 * una sugerencia sería duplicar un dato que cambia solo con cada compra.
 * Cuando la doctora acepta una sugerencia, ahí sí se crea un ítem real.
 */
@Entity
@Table(name = "shopping_list_items", indexes = {
    @Index(name = "idx_shopping_context", columnList = "context"),
    @Index(name = "idx_shopping_done", columnList = "done")
})
public class ShoppingListItem extends BaseEntity {

  @NotNull
  @Column(nullable = false, length = 200)
  private String description;

  @Column(length = 200)
  private String note;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashContext context;

  /** Producto del catálogo, si el ítem salió de una sugerencia. */
  @Column(name = "product_id")
  private UUID productId;

  @NotNull
  @Column(nullable = false)
  private boolean done = false;

  @Column(name = "done_at")
  private Instant doneAt;

  /** Email de quien lo anotó. */
  @Column(name = "created_by", length = 120)
  private String createdBy;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
  public CashContext getContext() { return context; }
  public void setContext(CashContext context) { this.context = context; }
  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }
  public boolean isDone() { return done; }
  public void setDone(boolean done) { this.done = done; }
  public Instant getDoneAt() { return doneAt; }
  public void setDoneAt(Instant doneAt) { this.doneAt = doneAt; }
  public String getCreatedBy() { return createdBy; }
  public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}