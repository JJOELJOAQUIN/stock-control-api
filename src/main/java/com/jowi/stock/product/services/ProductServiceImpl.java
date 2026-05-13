package com.jowi.stock.product.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jowi.stock.product.dto.CreateProductRequest;
import com.jowi.stock.product.dto.PatchProductRequest;
import com.jowi.stock.product.dto.ProductWithStockResponse;
import com.jowi.stock.product.dto.UpdateProductRequest;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.enums.ProductScope;
import com.jowi.stock.product.repositories.ProductRepository;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.repositories.StockRepository;


@Service
@Transactional
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final StockRepository stockRepository;

  public ProductServiceImpl(
      ProductRepository productRepository,
      StockRepository stockRepository) {
    this.productRepository = productRepository;
    this.stockRepository = stockRepository;
  }

  @Override
  public Product create(CreateProductRequest request) {

    if (request == null) {
      throw new IllegalArgumentException("CreateProductRequest cannot be null");
    }

    Product product = new Product();
    product.setName(request.name());
    product.setDescription(request.description());
    product.setMinimumStock(request.minimumStock());
    product.setCategory(request.category());
    product.setBrand(request.brand());
    product.setExpirable(
        request.expirable() != null ? request.expirable() : true);

    product.setScope(request.scope());
    product.setBarcode(request.barcode());
    product.setCostPrice(request.costPrice());

    validateProduct(product);

    return productRepository.save(product);
  }

  @Override
  public Product getById(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException(
            "Product not found: " + id));
  }

  @Override
  public List<Product> getAll() {
    return productRepository.findAll();
  }

  @Override
  public void deactivate(UUID id) {
    Product product = getById(id);
    product.setActive(false);
    productRepository.save(product);
  }

  @Override
  public Product update(UUID id, UpdateProductRequest r) {
    Product p = getById(id);

    p.setName(r.name());
    p.setDescription(r.description());
    p.setMinimumStock(r.minimumStock());
    p.setCategory(r.category());
    p.setBrand(r.brand());
    p.setExpirable(r.expirable());
    p.setActive(r.active());
    p.setCostPrice(r.costPrice());

    validateProduct(p);

    return productRepository.save(p);
  }

  @Override
  public Product updatePartial(UUID id, PatchProductRequest r) {
    Product p = getById(id);

    if (r.name() != null) p.setName(r.name());
    if (r.description() != null) p.setDescription(r.description());
    if (r.minimumStock() != null) p.setMinimumStock(r.minimumStock());
    if (r.category() != null) p.setCategory(r.category());
    if (r.brand() != null) p.setBrand(r.brand());
    if (r.expirable() != null) p.setExpirable(r.expirable());
    if (r.active() != null) p.setActive(r.active());

    validateProduct(p);

    return productRepository.save(p);
  }

  @Override
  public Product getByBarcode(String barcode) {
    if (barcode == null || barcode.isBlank()) {
      throw new IllegalArgumentException("Barcode is required");
    }

    return productRepository.findByBarcode(barcode)
        .orElseThrow(() -> new IllegalArgumentException("Product not found for barcode: " + barcode));
  }

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

  @Override
  @Transactional
  public void bulkCreate(List<CreateProductRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new IllegalArgumentException("Product list cannot be empty");
    }

    for (CreateProductRequest req : requests) {
      Product product = new Product();
      product.setName(req.name());
      product.setDescription(req.description());
      product.setMinimumStock(req.minimumStock());
      product.setCategory(req.category());
      product.setBrand(req.brand());
      product.setScope(req.scope());
      product.setExpirable(req.expirable() != null ? req.expirable() : true);
      product.setBarcode(req.barcode());
      product.setCostPrice(req.costPrice());

      validateProduct(product);

      productRepository.save(product);
    }
  }

  @Override
  @Transactional
  public void assignBarcode(UUID productId, String barcode) {
    if (barcode == null || barcode.isBlank()) {
      throw new IllegalArgumentException("Barcode is required");
    }

    Product product = getById(productId);

    productRepository.findByBarcode(barcode)
        .ifPresent(existing -> {
          if (!existing.getId().equals(productId)) {
            throw new IllegalStateException("Barcode already assigned to another product");
          }
        });

    product.setBarcode(barcode);
    productRepository.save(product);
  }

@Override
public List<ProductWithStockResponse> getAllWithStock(StockContext context) {
  return productRepository.findAll().stream()
      .filter(Product::getActive)
      .filter(product ->
          product.getScope() == ProductScope.BOTH ||
          product.getScope().name().equals(context.name()))
      .map(product -> {
        int currentStock = 0;
        boolean belowMinimum = true;

        var stockOpt = stockRepository.findByProductIdAndContext(product.getId(), context);

        if (stockOpt.isPresent()) {
          var stock = stockOpt.get();
          currentStock = stock.getCurrent();
          belowMinimum = stock.isBelowMinimum();
        }

        return new ProductWithStockResponse(
            product.getId(),
            product.getName(),
            product.getBarcode(),
            product.getBrand().name(),
            product.getCategory().name(),
            product.getScope().name(),
            product.getMinimumStock(),
            currentStock,
            belowMinimum,
            product.getActive(),
            product.getCostPrice()
        );
      })
      .toList();
}
}