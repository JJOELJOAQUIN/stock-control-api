package com.jowi.stock.shopping.controllers;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.shopping.dto.CreateShoppingItemRequest;
import com.jowi.stock.shopping.dto.ShoppingListItemResponse;
import com.jowi.stock.shopping.services.ShoppingListService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

  private final ShoppingListService service;

  public ShoppingListController(ShoppingListService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<ShoppingListItemResponse>> list(
      @RequestParam CashContext context) {
    return ResponseEntity.ok(
        service.list(context).stream().map(ShoppingListItemResponse::from).toList());
  }

  @PostMapping
  public ResponseEntity<ShoppingListItemResponse> add(
      @RequestBody CreateShoppingItemRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ShoppingListItemResponse.from(service.add(req)));
  }

  @PostMapping("/{id}/toggle")
  public ResponseEntity<ShoppingListItemResponse> toggle(@PathVariable UUID id) {
    return ResponseEntity.ok(ShoppingListItemResponse.from(service.toggle(id)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/done")
  public ResponseEntity<Void> clearDone(@RequestParam CashContext context) {
    service.clearDone(context);
    return ResponseEntity.noContent().build();
  }
}