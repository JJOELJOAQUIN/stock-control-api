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
  public Product create(Product product) {

    validateProduct(product);

    Product saved = productRepository.save(product);

    return saved;
  }

  @Override
  public Product getById(UUID id) {

    Product product =
        productRepository
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
