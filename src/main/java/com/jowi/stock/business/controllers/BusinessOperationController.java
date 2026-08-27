package com.jowi.stock.business.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jowi.stock.business.dto.CombinedSaleRequest;
import com.jowi.stock.business.dto.DermatoProcedureRequest;
import com.jowi.stock.business.dto.InternalConsumptionRequest;
import com.jowi.stock.business.dto.PurchaseOrderRequest;
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
  public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseOrderRequest req) {
    service.purchaseOrder(
        req.context(),
        req.comment(),
        req.paymentMethod(),
        req.expectedTotal(),
        req.items());

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

  @PostMapping("/combined-sale")
  public ResponseEntity<Void> combinedSale(@Valid @RequestBody CombinedSaleRequest req) {
    service.combinedSale(req);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * Consumo interno / uso personal / carrito-camilla: descuenta stock con
   * trazabilidad, sin impacto en caja ni en métricas de ventas.
   */
  @PostMapping("/internal-consumption")
  public ResponseEntity<Void> internalConsumption(
      @Valid @RequestBody InternalConsumptionRequest req) {
    service.internalConsumption(req);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * Sesión de tratamiento dermatológico: caja (100% médica) + consumo de
   * insumos del recetario en una sola operación atómica.
   */
  @PostMapping("/dermato-procedure")
  public ResponseEntity<Void> dermatoProcedure(
      @Valid @RequestBody DermatoProcedureRequest req) {
    service.dermatoProcedure(req);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
