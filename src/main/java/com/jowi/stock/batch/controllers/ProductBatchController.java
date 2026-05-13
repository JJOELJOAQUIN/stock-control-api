package com.jowi.stock.batch.controllers;

import com.jowi.stock.batch.dto.ProductBatchExpirationResponse;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.stock.enums.StockContext;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-batches")
public class ProductBatchController {

  private final ProductBatchService service;

  public ProductBatchController(ProductBatchService service) {
    this.service = service;
  }

  @GetMapping("/expiring")
  public ResponseEntity<List<ProductBatchExpirationResponse>> expiring(
      @RequestParam StockContext context,
      @RequestParam(defaultValue = "30") int days
  ) {
    return ResponseEntity.ok(service.getExpiring(context, days));
  }
}