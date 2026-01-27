package com.jowi.stock.product;

import java.util.List;
import java.util.UUID;

public interface ProductService {

  Product create(CreateProductRequest request);

  Product getById(UUID id);

  List<Product> getAll();

  void deactivate(UUID id);

  Product update(UUID id, UpdateProductRequest request);

  Product updatePartial(UUID id, PatchProductRequest request);

}
