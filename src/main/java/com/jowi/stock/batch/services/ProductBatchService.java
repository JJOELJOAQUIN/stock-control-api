package com.jowi.stock.batch.services;

import com.jowi.stock.batch.dto.ProductBatchExpirationResponse;
import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.batch.repositories.ProductBatchRepository;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductBatchService {

  private final ProductBatchRepository repository;
  private final ProductService productService;

  public ProductBatchService(
      ProductBatchRepository repository,
      ProductService productService
  ) {
    this.repository = repository;
    this.productService = productService;
  }

  public ProductBatch createBatch(
      UUID productId,
      StockContext context,
      int quantity,
      LocalDate expirationDate,
      String lotNumber
  ) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Batch quantity must be greater than zero");
    }

    Product product = productService.getById(productId);

    if (Boolean.TRUE.equals(product.getExpirable()) && expirationDate == null) {
      throw new IllegalArgumentException("Expiration date is required for expirable products");
    }

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setContext(context);
    batch.setQuantityInitial(quantity);
    batch.setQuantityCurrent(quantity);
    batch.setExpirationDate(expirationDate);
    batch.setLotNumber(lotNumber);

    return repository.save(batch);
  }

  @Transactional(readOnly = true)
  public List<ProductBatchExpirationResponse> getExpiring(
      StockContext context,
      int days
  ) {
    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }

    if (days <= 0) {
      throw new IllegalArgumentException("days must be greater than zero");
    }

    LocalDate today = LocalDate.now();
    LocalDate limit = today.plusDays(days);

    return repository
        .findByContextAndExpirationDateBetweenAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
            context,
            today,
            limit,
            0
        )
        .stream()
        .map(ProductBatchExpirationResponse::from)
        .toList();
  }
}
