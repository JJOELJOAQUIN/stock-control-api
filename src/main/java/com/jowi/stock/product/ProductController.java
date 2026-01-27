package com.jowi.stock.product;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
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

}
