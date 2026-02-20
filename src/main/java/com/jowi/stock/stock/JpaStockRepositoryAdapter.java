package com.jowi.stock.stock;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaStockRepositoryAdapter implements StockRepository {

  private final JpaStockRepository jpaRepository;
  private final ProductRepository productRepository;

  public JpaStockRepositoryAdapter(
      JpaStockRepository jpaRepository,
      ProductRepository productRepository) {
    this.jpaRepository = jpaRepository;
    this.productRepository = productRepository;
  }

  @Override
  public Optional<Stock> findByProductIdAndContext(
      UUID productId,
      StockContext context) {

    return jpaRepository
        .findByProduct_IdAndContext(productId, context)
        .map(e -> new Stock(
            e.getProduct().getId(),
            e.getCurrent(),
            e.getProduct().getMinimumStock()
        ));
  }

  @Override
  public void save(UUID productId, StockContext context, int current) {

    Product product = productRepository.findById(productId)
        .orElseThrow(() ->
            new IllegalStateException("Product not found: " + productId));

    StockEntity entity = jpaRepository
        .findByProduct_IdAndContext(productId, context)
        .orElse(new StockEntity(product, context, current));

    entity.setCurrent(current);

    jpaRepository.save(entity);
  }

  @Override
  public boolean existsByProductIdAndContext(
      UUID productId,
      StockContext context) {

    return jpaRepository.existsByProduct_IdAndContext(productId, context);
  }

  @Override
  public List<Stock> findAllByContext(StockContext context) {

    return jpaRepository.findByContext(context).stream()
        .map(e -> new Stock(
            e.getProduct().getId(),
            e.getCurrent(),
            e.getProduct().getMinimumStock()
        ))
        .toList();
  }
}
