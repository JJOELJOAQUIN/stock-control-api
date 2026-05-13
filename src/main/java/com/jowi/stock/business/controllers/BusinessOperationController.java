package com.jowi.stock.business.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.business.dto.PurchaseProductRequest;
import com.jowi.stock.business.dto.SellByBarcodeRequest;
import com.jowi.stock.business.dto.SellProductRequest;
import com.jowi.stock.business.services.BusinessOperationService;

@RestController
@RequestMapping("/api/business")
public class BusinessOperationController {

  private final BusinessOperationService service;

  public BusinessOperationController(BusinessOperationService service) {
    this.service = service;
  }

  @PostMapping("/sell")
  public ResponseEntity<Void> sell(@Valid @RequestBody SellProductRequest req) {
    service.sellProduct(
        req.productId(),
        req.quantity(),
        req.amount(),
        req.paymentMethod(),
        req.context(),
        req.comment());

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/purchase")
  public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseProductRequest req) {
    service.purchaseProduct(
        req.productId(),
        req.quantity(),
        req.amount(),
        req.context(),
        req.comment(),
        req.expirationDate(),
        req.lotNumber());

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/sell-by-barcode")
  public ResponseEntity<Void> sellByBarcode(
      @Valid @RequestBody SellByBarcodeRequest req) {

    service.sellByBarcode(
        req.barcode(),
        req.quantity(),
        req.amount(),
        req.paymentMethod(),
        req.context(),
        req.comment(),
        req.performedBy());

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}