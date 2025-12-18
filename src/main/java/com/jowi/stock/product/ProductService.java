package com.jowi.stock.product;

import java.util.List;
import java.util.UUID;

public interface ProductService {

  Product create(Product product);

  Product getById(UUID id);

  List<Product> getAll();

  void deactivate(UUID id);
}
