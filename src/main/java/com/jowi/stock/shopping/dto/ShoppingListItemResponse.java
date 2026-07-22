package com.jowi.stock.shopping.dto;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.shopping.entities.ShoppingListItem;
import java.time.Instant;
import java.util.UUID;

public record ShoppingListItemResponse(
    UUID id,
    String description,
    String note,
    CashContext context,
    UUID productId,
    boolean done,
    Instant doneAt,
    String createdBy,
    Instant createdAt) {

  public static ShoppingListItemResponse from(ShoppingListItem i) {
    return new ShoppingListItemResponse(
        i.getId(), i.getDescription(), i.getNote(), i.getContext(),
        i.getProductId(), i.isDone(), i.getDoneAt(), i.getCreatedBy(),
        i.getCreatedAt());
  }
}