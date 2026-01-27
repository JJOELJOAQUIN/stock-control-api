package com.jowi.stock.stock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

  Optional<Stock> findByProductId(UUID productId);

  void save(Stock stock);

  boolean existsByProductId(UUID productId);

 
   List<Stock> findAll();
}
