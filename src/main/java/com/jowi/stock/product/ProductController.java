package com.jowi.stock.product;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  public ResponseEntity<Product> create(@RequestBody Product product) {

    Product createdProduct;
    createdProduct = productService.create(product);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
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
}
