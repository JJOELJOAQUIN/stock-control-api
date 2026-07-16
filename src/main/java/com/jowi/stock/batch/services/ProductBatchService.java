package com.jowi.stock.batch.services;

import com.jowi.stock.batch.dto.BatchAllocation;
import com.jowi.stock.batch.dto.ProductBatchExpirationResponse;
import com.jowi.stock.batch.entities.ProductBatch;
import com.jowi.stock.batch.repositories.ProductBatchRepository;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
   * Devuelve el detalle de qué se tomó de cada lote, para que la salida quede
   * trazada y una eventual anulación pueda devolver cada unidad a su lote.
   *
   * Tolerante a datos legacy: si el stock histórico no tiene lote asociado
   * (ingresos previos al sistema de lotes o por /api/stock/{id}/in), consume
   * lo que haya y no falla — en ese caso la lista devuelta cubre menos
   * unidades que las pedidas, o viene vacía.
   */
  public List<BatchAllocation> consume(UUID productId, StockContext context, int quantity) {
    if (quantity <= 0) {
      return List.of();
    }

    List<ProductBatch> batches = repository
        .findByProductIdAndContextAndQuantityCurrentGreaterThanOrderByExpirationDateAsc(
            productId, context, 0);

    List<BatchAllocation> allocations = new ArrayList<>();
    int remaining = quantity;

    for (ProductBatch batch : batches) {
      if (remaining <= 0) {
        break;
      }

      int take = Math.min(batch.getQuantityCurrent(), remaining);
      batch.setQuantityCurrent(batch.getQuantityCurrent() - take);
      remaining -= take;

      allocations.add(new BatchAllocation(batch.getId(), take));
    }

    repository.saveAll(batches);

    return allocations;
  }

  /**
   * Devuelve unidades a un lote puntual. Se usa al anular una venta con
   * trazabilidad: cada unidad vuelve al lote del que salió.
   *
   * No se topea contra quantityInitial a propósito: un lote puede terminar
   * con más unidades que las iniciales si se reconcilió stock a mano, y
   * preferimos que el número refleje la realidad antes que una invariante
   * que nadie mira.
   */
  public void restore(UUID batchId, int quantity) {
    if (quantity <= 0) {
      return;
    }

    ProductBatch batch = repository.findById(batchId)
        .orElseThrow(() -> new EntityNotFoundException("Batch not found: " + batchId));

    batch.setQuantityCurrent(batch.getQuantityCurrent() + quantity);
    repository.save(batch);
  }

  /**
   * Devolución aproximada, para ventas anteriores a la trazabilidad de lotes:
   * no sabemos de qué lote salieron, así que las unidades vuelven al lote de
   * vencimiento MÁS TARDÍO. Es el criterio conservador: nunca inventa un
   * vencimiento próximo que dispare una alerta falsa.
   *
   * Devuelve true si pudo asignarlas a algún lote; false si el producto no
   * tiene lotes (stock legacy suelto), caso en el que solo se ajusta el stock.
   */
  public boolean restoreApproximate(UUID productId, StockContext context, int quantity) {
    if (quantity <= 0) {
      return true;
    }

    List<ProductBatch> batches = repository.findByProductIdAndContext(productId, context);

    if (batches.isEmpty()) {
      return false;
    }

    ProductBatch target = batches.stream()
        .max((a, b) -> {
          LocalDate ea = a.getExpirationDate();
          LocalDate eb = b.getExpirationDate();
          // Sin fecha = "no vence": es el destino más conservador de todos.
          if (ea == null && eb == null) {
            return a.getCreatedAt().compareTo(b.getCreatedAt());
          }
          if (ea == null) {
            return 1;
          }
          if (eb == null) {
            return -1;
          }
          return ea.compareTo(eb);
        })
        .orElseThrow();

    target.setQuantityCurrent(target.getQuantityCurrent() + quantity);
    repository.save(target);

    return true;
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