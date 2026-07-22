package com.jowi.stock.shopping.services;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.shopping.dto.CreateShoppingItemRequest;
import com.jowi.stock.shopping.entities.ShoppingListItem;
import com.jowi.stock.shopping.repositories.ShoppingListRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ShoppingListService {

  private final ShoppingListRepository repository;
  private final CurrentUserService currentUserService;

  public ShoppingListService(
      ShoppingListRepository repository,
      CurrentUserService currentUserService) {
    this.repository = repository;
    this.currentUserService = currentUserService;
  }

  public List<ShoppingListItem> list(CashContext context) {
    if (context == null) throw new IllegalArgumentException("context is required");
    return repository.findByContextOrderByDoneAscCreatedAtDesc(context);
  }

  public ShoppingListItem add(CreateShoppingItemRequest req) {
    if (req.description() == null || req.description().isBlank()) {
      throw new IllegalArgumentException("La descripción es obligatoria");
    }
    if (req.context() == null) throw new IllegalArgumentException("context is required");

    ShoppingListItem item = new ShoppingListItem();
    item.setDescription(req.description().trim());
    item.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
    item.setContext(req.context());
    item.setProductId(req.productId());
    item.setCreatedBy(currentUserService.currentUserLabel());

    return repository.save(item);
  }

  /** Tachar / destachar. A diferencia de la anulación de caja, esto sí es reversible. */
  public ShoppingListItem toggle(UUID id) {
    ShoppingListItem item = repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Shopping item not found: " + id));

    item.setDone(!item.isDone());
    item.setDoneAt(item.isDone() ? Instant.now() : null);

    return repository.save(item);
  }

  /** Borrado real: es una libreta, no un registro contable. */
  public void delete(UUID id) {
    repository.deleteById(id);
  }

  /** Limpia lo ya comprado. Botón de "limpiar tachados". */
  public void clearDone(CashContext context) {
    List<ShoppingListItem> done = repository
        .findByContextOrderByDoneAscCreatedAtDesc(context)
        .stream().filter(ShoppingListItem::isDone).toList();
    repository.deleteAll(done);
  }
}