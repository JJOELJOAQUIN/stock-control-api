package com.jowi.stock.stock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class JpaStockRepositoryAdapter implements StockRepository {

  private final JpaStockRepository jpaRepository;

  public JpaStockRepositoryAdapter(JpaStockRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Stock> findByProductId(UUID productId) {
    return jpaRepository.findById(productId)
        .map(e -> new Stock(
            e.getProductId(),
            e.getCurrent(),
            e.getMinimum()
        ));
  }

  @Override
  public void save(Stock stock) {
    StockEntity entity = new StockEntity(
        stock.getProductId(),
        stock.getCurrent(),
        stock.getMinimum()
    );
    jpaRepository.save(entity);
  }

  @Override
  public boolean existsByProductId(UUID productId) {
    return jpaRepository.existsById(productId);
  }

    @Override
  public List<Stock> findAll() {
    return jpaRepository.findAll().stream()
        .map(e -> new Stock(
            e.getProductId(),
            e.getCurrent(),
            e.getMinimum()
        ))
        .collect(Collectors.toList());
  }
}