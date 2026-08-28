package com.jowi.stock.product.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jowi.stock.product.dto.CreateProductRequest;
import com.jowi.stock.product.dto.PatchProductRequest;
import com.jowi.stock.product.dto.ProductWithStockResponse;
import com.jowi.stock.product.dto.UpdateProductRequest;
import com.jowi.stock.procedure.entities.ProcedureConsumption;
import com.jowi.stock.procedure.repositories.ProcedureCatalogRepository;
import com.jowi.stock.procedure.repositories.ProcedureConsumptionRepository;
import com.jowi.stock.product.dto.CreateProductRequest.ProductRecipeLineRequest;
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
  private final ProcedureConsumptionRepository procedureConsumptionRepository;
  private final ProcedureCatalogRepository procedureCatalogRepository;

  public ProductServiceImpl(
      ProductRepository productRepository,
      StockRepository stockRepository,
      ProcedureConsumptionRepository procedureConsumptionRepository,
      ProcedureCatalogRepository procedureCatalogRepository) {
    this.productRepository = productRepository;
    this.stockRepository = stockRepository;
    this.procedureConsumptionRepository = procedureConsumptionRepository;
    this.procedureCatalogRepository = procedureCatalogRepository;
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
    product.setSalePrice(request.salePrice());
    product.setDefaultMarkupPercentage(request.defaultMarkupPercentage());
    product.setShelfLifeMonths(request.shelfLifeMonths());
    product.setRestockPriority(request.restockPriority());
    product.setConsumptionUnit(request.consumptionUnit());
    product.setUnitsPerPackage(request.unitsPerPackage());

    validateProduct(product);

    Product saved = productRepository.save(product);

    // Asociación opcional a procedimientos (receta/BOM). Misma transacción:
    // si una línea falla, no queda ni el producto a medias ni recetas sueltas.
    persistRecipes(saved, request.recipes());

    return saved;
  }

  /**
   * Crea los renglones de receta (procedure_consumption) que asocian este
   * producto a uno o varios procedimientos. Valida que cada procedureCode
   * exista en el catálogo y respeta el unique (procedure_code, product_id):
   * si ya hay una línea para ese par, actualiza la cantidad en vez de duplicar.
   */
  private void persistRecipes(Product product, List<ProductRecipeLineRequest> recipes) {
    if (recipes == null || recipes.isEmpty()) {
      return;
    }

    for (ProductRecipeLineRequest line : recipes) {
      if (line == null) {
        continue;
      }
      String code = line.procedureCode() == null ? null : line.procedureCode().trim();
      if (code == null || code.isBlank()) {
        throw new IllegalArgumentException("procedureCode requerido en la receta");
      }
      if (line.quantity() == null || line.quantity() < 1) {
        throw new IllegalArgumentException(
            "La cantidad de la receta para " + code + " debe ser >= 1");
      }
      if (!procedureCatalogRepository.existsByCodeIgnoreCase(code)) {
        throw new IllegalArgumentException(
            "El procedimiento " + code + " no existe en el catálogo");
      }

      // Upsert por (procedureCode, productId) para respetar el unique.
      ProcedureConsumption row = procedureConsumptionRepository
          .findByProcedureCode(code).stream()
          .filter(c -> c.getProductId().equals(product.getId()))
          .findFirst()
          .orElseGet(ProcedureConsumption::new);

      row.setProcedureCode(code);
      row.setProductId(product.getId());
      row.setQuantity(line.quantity());
      procedureConsumptionRepository.save(row);
    }
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
    p.setSalePrice(r.salePrice());
    p.setDefaultMarkupPercentage(r.defaultMarkupPercentage());
    p.setShelfLifeMonths(r.shelfLifeMonths());

    // Solo pisamos la prioridad si el cliente la envió: evita que clientes
    // viejos (sin el campo) la reseteen a 0 en cada edición.
    if (r.restockPriority() != null) {
      p.setRestockPriority(r.restockPriority());
    }

    validateProduct(p);

    return productRepository.save(p);
  }

  @Override
  public Product updatePartial(UUID id, PatchProductRequest r) {
    Product p = getById(id);

    if (r.name() != null)
      p.setName(r.name());
    if (r.description() != null)
      p.setDescription(r.description());
    if (r.minimumStock() != null)
      p.setMinimumStock(r.minimumStock());
    if (r.category() != null)
      p.setCategory(r.category());
    if (r.brand() != null)
      p.setBrand(r.brand());
    if (r.expirable() != null)
      p.setExpirable(r.expirable());
    if (r.active() != null)
      p.setActive(r.active());
    if (r.costPrice() != null)
      p.setCostPrice(r.costPrice());

    if (r.salePrice() != null)
      p.setSalePrice(r.salePrice());

    if (r.defaultMarkupPercentage() != null)
      p.setDefaultMarkupPercentage(r.defaultMarkupPercentage());

    if (r.shelfLifeMonths() != null)
      p.setShelfLifeMonths(r.shelfLifeMonths());

    if (r.restockPriority() != null)
      p.setRestockPriority(r.restockPriority());

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

    if (product.getShelfLifeMonths() != null && product.getShelfLifeMonths() <= 0) {
      throw new IllegalArgumentException("Shelf life months must be greater than zero");
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
      product.setSalePrice(req.salePrice());
      product.setDefaultMarkupPercentage(req.defaultMarkupPercentage());
      product.setShelfLifeMonths(req.shelfLifeMonths());
      product.setRestockPriority(req.restockPriority());
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
        .filter(product -> product.getScope() == ProductScope.BOTH ||
            product.getScope().name().equals(context.name()))
        .map(product -> {
          int currentStock = 0;

          var stockOpt = stockRepository.findByProductIdAndContext(product.getId(), context);

          if (stockOpt.isPresent()) {
            currentStock = stockOpt.get().getCurrent();
          }

          // FIX: antes, un producto sin fila de stock quedaba con
          // belowMinimum = true aunque su mínimo fuera 0, y eso inflaba el
          // aviso de "stock bajo". La regla es una sola y se aplica siempre:
          // está bajo si el stock actual (0 si no hay fila) está por debajo
          // del mínimo configurado.
          boolean belowMinimum = currentStock < product.getMinimumStock();

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
              product.getCostPrice(),
              product.getSalePrice(),
              product.getDefaultMarkupPercentage(),
              product.getShelfLifeMonths(),
              product.getRestockPriority(),
              product.getConsumptionUnit() == null
                  ? null
                  : product.getConsumptionUnit().name());
        })
        .toList();
  }
}
