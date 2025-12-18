package com.jowi.stock.stock;

import java.util.UUID;

public interface StockRepository {

  Stock getByProductId(UUID productId);
}
