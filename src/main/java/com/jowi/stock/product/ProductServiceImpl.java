package com.jowi.stock.product;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;

  public ProductServiceImpl(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Product create(CreateProductRequest request) {

    if (request == null) {
      throw new IllegalArgumentException("CreateProductRequest cannot be null");
    }

    Product product = new Product();
    product.setName(request.name());
    product.setDescription(request.description());
    product.setMinimumStock(request.minimumStock());
    product.setCategory(request.category());
    product.setBrand(request.brand());
    product.setExpirable(
        request.expirable() != null ? request.expirable() : true);

    validateProduct(product);

    return productRepository.save(product);
  }

  @Override
  public Product getById(UUID id) {

    Product product = productRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException(
            "Product not found: " + id));

    return product;
  }

  @Override
  public List<Product> getAll() {

    List<Product> products = productRepository.findAll();

    return products;
  }

  @Override
  public void deactivate(UUID id) {

    Product product = getById(id);

    product.setActive(false);

    productRepository.save(product);
  }

  @Override
  public Product update(UUID id, UpdateProductRequest r) {

    Product p = getById(id);

    p.setName(r.name());
    p.setDescription(r.description());
    p.setMinimumStock(r.minimumStock());
    p.setCategory(r.category());
    p.setBrand(r.brand());
    p.setExpirable(r.expirable());
    p.setActive(r.active());

    validateProduct(p);

    return productRepository.save(p);
  }

  @Override
  public Product updatePartial(UUID id, PatchProductRequest r) {

    Product p = getById(id);

    if (r.name() != null)
      p.setName(r.name());
    if (r.description() != null)
      p.setDescription(r.description());
    if (r.minimumStock() != null)
      p.setMinimumStock(r.minimumStock());
    if (r.category() != null)
      p.setCategory(r.category());
    if (r.brand() != null)
      p.setBrand(r.brand());
    if (r.expirable() != null)
      p.setExpirable(r.expirable());
    if (r.active() != null)
      p.setActive(r.active());

    validateProduct(p);

    return productRepository.save(p);
  }

  // ========================
  // VALIDACIONES DE DOMINIO
  // ========================
  private void validateProduct(Product product) {

    if (product == null) {
      throw new IllegalArgumentException("Product cannot be null");
    }

    if (product.getName() == null || product.getName().isBlank()) {
      throw new IllegalArgumentException("Product name is required");
    }

    if (product.getMinimumStock() == null) {
      throw new IllegalArgumentException("Minimum stock is required");
    }

    if (product.getMinimumStock() < 0) {
      throw new IllegalArgumentException("Minimum stock cannot be negative");
    }
  }
}
