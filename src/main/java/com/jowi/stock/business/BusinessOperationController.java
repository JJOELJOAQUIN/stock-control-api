package com.jowi.stock.business;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
public class BusinessOperationController {

  private final BusinessOperationService service;

  public BusinessOperationController(BusinessOperationService service) {
    this.service = service;
  }

  // =========================
  // VENTA DE PRODUCTO
  // =========================
  @PostMapping("/sell")
  public ResponseEntity<Void> sell(@Valid @RequestBody SellProductRequest req) {

    service.sellProduct(
        req.productId(),
        req.quantity(),
        req.amount(),
        req.paymentMethod(),
        req.context(),
        req.comment()
    );

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // =========================
  // COMPRA A PROVEEDOR
  // =========================
  @PostMapping("/purchase")
  public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseProductRequest req) {

    service.purchaseProduct(
        req.productId(),
        req.quantity(),
        req.amount(),
        req.comment()
    );

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
