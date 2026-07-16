package com.jowi.stock.batch.services;

import com.jowi.stock.batch.dto.ProductBatchExpirationResponse;
import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.batch.repositories.ProductBatchRepository;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductBatchService {

  private final ProductBatchRepository repository;
  private final ProductService productService;

  public ProductBatchService(
      ProductBatchRepository repository,
      ProductService productService
  ) {
    this.repository = repository;
    this.productService = productService;
  }

  public ProductBatch createBatch(
      UUID productId,
      StockContext context,
      int quantity,
      LocalDate expirationDate,
      String lotNumber
  ) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Batch quantity must be greater than zero");
    }

    Product product = productService.getById(productId);

    LocalDate effectiveExpiration = expirationDate;
    boolean estimated = false;

    // Productos magistrales: si no se cargó vencimiento manual pero el
    // producto tiene vida útil configurada (meses), se estima el vencimiento
    // desde la fecha de ingreso al consultorio.
    if (effectiveExpiration == null
        && product.getShelfLifeMonths() != null
        && product.getShelfLifeMonths() > 0) {
      effectiveExpiration = LocalDate.now().plusMonths(product.getShelfLifeMonths());
      estimated = true;
    }

    if (Boolean.TRUE.equals(product.getExpirable()) && effectiveExpiration == null) {
      throw new IllegalArgumentException(
          "El producto es vencible: cargá la fecha de vencimiento en la compra "
              + "o configurá su vida útil estimada (meses) en la ficha del producto");
    }

    ProductBatch batch = new ProductBatch();
    batch.setProduct(product);
    batch.setContext(context);
    batch.setQuantityInitial(quantity);
    batch.setQuantityCurrent(quantity);
    batch.setExpirationDate(effectiveExpiration);
    batch.setExpirationEstimated(estimated);
    batch.setLotNumber(lotNumber);

    return repository.save(batch);
  }

  /**
   * Consume lotes en orden FEFO (primero los que vencen antes; los lotes sin
   * fecha quedan al final por el NULLS LAST de Postgres). Se llama en cada
   * salida de stock (venta, venta combinada, consumo interno) para que
   * quantityCurrent de los lotes refleje el stock real y los avisos de
   * "próximo a vencer" no muestren lotes ya utilizados (caso NCTF 130 HA).
   *
   * Tolerante a datos legacy: si el stock histórico no tiene lote asociado
   * (ingresos previos al sistema de lotes o por /api/stock/{id}/in), consume
   * lo que haya y no falla.
   */
  public void consume(UUID productId, StockContext context, int quantity) {
    if (quantity <= 0) {
      return;
    }

    List<ProductBatch> batches = repository
        .findByProductIdAndContextAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
            productId, context, 0);

    int remaining = quantity;

    for (ProductBatch batch : batches) {
      if (remaining <= 0) {
        break;
      }

      int take = Math.min(batch.getQuantityCurrent(), remaining);
      batch.setQuantityCurrent(batch.getQuantityCurrent() - take);
      remaining -= take;
    }

    repository.saveAll(batches);
  }

  @Transactional(readOnly = true)
  public List<ProductBatchExpirationResponse> getExpiring(
      StockContext context,
      int days
  ) {
    if (context == null) {
      throw new IllegalArgumentException("context is required");
    }

    if (days <= 0) {
      throw new IllegalArgumentException("days must be greater than zero");
    }

    LocalDate today = LocalDate.now();
    LocalDate limit = today.plusDays(days);

    return repository
        .findByContextAndExpirationDateBetweenAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
            context,
            today,
            limit,
            0
        )
        .stream()
        .map(ProductBatchExpirationResponse::from)
        .toList();
  }
}