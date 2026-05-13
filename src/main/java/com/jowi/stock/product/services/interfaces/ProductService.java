package com.jowi.stock.product.services.interfaces;

import java.util.List;
import java.util.UUID;

import com.jowi.stock.product.dto.CreateProductRequest;
import com.jowi.stock.product.dto.PatchProductRequest;
import com.jowi.stock.product.dto.ProductWithStockResponse;
import com.jowi.stock.product.dto.UpdateProductRequest;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.stock.enums.StockContext;

public interface ProductService {

  Product create(CreateProductRequest request);

  Product getById(UUID id);

  List<Product> getAll();

  void deactivate(UUID id);

  Product update(UUID id, UpdateProductRequest request);

  Product updatePartial(UUID id, PatchProductRequest request);

  Product getByBarcode(String barcode);

  void bulkCreate(List<CreateProductRequest> requests);
  void assignBarcode(UUID productId, String barcode);
 List<ProductWithStockResponse> getAllWithStock(StockContext context);


}
