package com.jowi.stock.stock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {

  Optional<Stock> findByProductIdAndContext(UUID productId, StockContext context);

  void save(UUID productId, StockContext context, int current);

  boolean existsByProductIdAndContext(UUID productId, StockContext context);

  List<Stock> findAllByContext(StockContext context);
}

