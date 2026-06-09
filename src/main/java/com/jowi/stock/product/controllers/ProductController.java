package com.jowi.stock.product.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.product.dto.AssignBarcodeRequest;
import com.jowi.stock.product.dto.CreateProductRequest;
import com.jowi.stock.product.dto.PatchProductRequest;
import com.jowi.stock.product.dto.ProductScanResponse;
import com.jowi.stock.product.dto.ProductScanWithStockResponse;
import com.jowi.stock.product.dto.ProductWithStockResponse;
import com.jowi.stock.product.dto.UpdateProductRequest;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.entities.Stock;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.services.StockService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  private final StockService stockService;

  public ProductController(ProductService productService, StockService stockService) {
    this.productService = productService;
    this.stockService = stockService;
  }

  @PostMapping
  public ResponseEntity<Product> create(
      @Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(productService.create(request));
  }

  @GetMapping
  public ResponseEntity<List<Product>> getAll() {

    List<Product> products;
    products = productService.getAll();

    return ResponseEntity.ok(products);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Product> getById(@PathVariable UUID id) {

    Product product;
    product = productService.getById(id);

    return ResponseEntity.ok(product);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {

    productService.deactivate(id);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<Product> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProductRequest request) {
    return ResponseEntity.ok(productService.update(id, request));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Product> updatePartial(
      @PathVariable UUID id,
      @RequestBody PatchProductRequest request) {
    return ResponseEntity.ok(productService.updatePartial(id, request));
  }

  @PostMapping("/bulk")
  public ResponseEntity<Void> bulkCreate(
      @RequestBody List<CreateProductRequest> requests) {

    productService.bulkCreate(requests);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @GetMapping("/scan/{barcode}")
  public ResponseEntity<ProductScanResponse> scan(
      @PathVariable String barcode) {
    Product product = productService.getByBarcode(barcode);
    return ResponseEntity.ok(ProductScanResponse.from(product));
  }

  @PatchMapping("/{id}/barcode")
  public ResponseEntity<Void> assignBarcode(
      @PathVariable UUID id,
      @RequestBody @Valid AssignBarcodeRequest request) {

    productService.assignBarcode(id, request.barcode());

    return ResponseEntity.ok().build();
  }

  @GetMapping("/with-stock")
  public ResponseEntity<List<ProductWithStockResponse>> getAllWithStock(
      @RequestParam StockContext context) {
    return ResponseEntity.ok(productService.getAllWithStock(context));
  }

  @GetMapping("/scan")
  public ResponseEntity<ProductScanWithStockResponse> scanWithContext(
      @RequestParam String barcode,
      @RequestParam StockContext context) {

    Product product = productService.getByBarcode(barcode);

    Stock stock;

    try {
      stock = stockService.getStock(product.getId(), context);
    } catch (Exception e) {
      stockService.initStock(product.getId(), context, 0);
      stock = stockService.getStock(product.getId(), context);
    }

    return ResponseEntity.ok(
        new ProductScanWithStockResponse(
            product.getId().toString(),
            product.getName(),
            product.getBarcode(),
            product.getScope().name(),
            stock.getCurrent(),
            stock.isBelowMinimum(),
            product.getCostPrice(),
            product.getSalePrice(),
            product.getDefaultMarkupPercentage()));
  }

}
