package com.jowi.stock.business.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.jowi.stock.batch.services.ProductBatchService;
import com.jowi.stock.business.dto.PurchaseItemRequest;
import com.jowi.stock.cash.dto.CreateCashMovementRequest;
import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.services.CashMovementService;
import com.jowi.stock.product.services.interfaces.ProductService;
import com.jowi.stock.stock.enums.StockContext;
import com.jowi.stock.stock.services.StockService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class BusinessOperationService {

  private final StockService stockService;
  private final CashMovementService cashService;
  private final ProductService productService;
  private final ProductBatchService batchService;

  public BusinessOperationService(
      StockService stockService,
      CashMovementService cashService,
      ProductService productService,
      ProductBatchService batchService) {
    this.stockService = stockService;
    this.cashService = cashService;
    this.productService = productService;
    this.batchService = batchService;
  }

  public void sellProduct(
      UUID productId,
      int quantity,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      String comment) {

    productService.getById(productId);

    stockService.decrease(productId, context.toStockContext(), quantity);

    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PRODUCT_SALE,
            paymentMethod,
            context,
            amount,
            null,
            comment,
            productId,
            null,
            null,
            null));
  }

  /**
   * Procesa una orden de compra multi-ítem de forma atómica.
   *
   * Por cada ítem: asegura el stock, lo incrementa, crea el lote y actualiza
   * los precios del producto si corresponde. Acumula el subtotal de cada ítem
   * y, al final, valida que la suma coincida con el total esperado por el
   * cliente y crea un ÚNICO movimiento de caja (OUT) por el total.
   *
   * Toda la operación corre en la transacción de la clase: si un ítem falla,
   * se revierte la compra completa.
   *
   * @param context        contexto de caja/stock
   * @param comment         comentario general (proveedor / observación)
   * @param paymentMethod   método de pago de la compra
   * @param expectedTotal   total calculado por el cliente, validado contra el cálculo del backend
   * @param items           detalle de ítems (mínimo uno)
   */
  public void purchaseOrder(
      CashContext context,
      String comment,
      PaymentMethod paymentMethod,
      BigDecimal expectedTotal,
      List<PurchaseItemRequest> items) {

    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("La compra debe tener al menos un ítem");
    }

    StockContext stockContext = context.toStockContext();

    BigDecimal computedTotal = BigDecimal.ZERO;

    for (PurchaseItemRequest item : items) {
      computedTotal = computedTotal.add(processPurchaseItem(item, stockContext));
    }

    // Integridad: el total real (calculado desde los ítems) es la fuente de
    // verdad. Se valida contra el total que envió el cliente, con tolerancia
    // de un centavo para diferencias de redondeo.
    BigDecimal difference = computedTotal.subtract(expectedTotal).abs();
    if (difference.compareTo(new BigDecimal("0.01")) > 0) {
      throw new IllegalArgumentException(
          "El total de la compra no coincide con la suma de los ítems");
    }

    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.OUT,
            CashSource.PROVIDER_PAYMENT,
            paymentMethod,
            context,
            computedTotal,
            BigDecimal.ZERO,
            comment,
            null,
            null,
            null,
            null));
  }

  /**
   * Procesa un único ítem de la compra (stock + lote + precios) y devuelve su
   * subtotal. No crea movimiento de caja: ese se hace una sola vez por la
   * orden completa.
   */
  private BigDecimal processPurchaseItem(
      PurchaseItemRequest item, StockContext stockContext) {

    UUID productId = item.productId();
    int quantity = item.quantity();

    var product = productService.getById(productId);

    if (!stockService.exists(productId, stockContext)) {
      stockService.initStock(productId, stockContext, 0);
    }

    stockService.increase(productId, stockContext, quantity);

    batchService.createBatch(
        productId,
        stockContext,
        quantity,
        item.expirationDate(),
        item.lotNumber());

    BigDecimal unitCost = item.unitCost();

    if (Boolean.TRUE.equals(item.updateCostPrice())) {
      product.setCostPrice(unitCost);
    }

    if (Boolean.TRUE.equals(item.updateSalePrice()) && item.newSalePrice() != null) {
      product.setSalePrice(item.newSalePrice());
    }

    if (Boolean.TRUE.equals(item.updateMarkupPercentage())) {
      if (item.newDefaultMarkupPercentage() == null) {
        throw new IllegalArgumentException("newDefaultMarkupPercentage is required");
      }

      product.setDefaultMarkupPercentage(item.newDefaultMarkupPercentage());
    }

    return item.subtotal();
  }

  public void sellByBarcode(
      String barcode,
      int quantity,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      String comment,
      CashActor performedBy) {

    var product = productService.getByBarcode(barcode);

    if (product.getCostPrice() != null) {
      BigDecimal expected = product.getCostPrice()
          .multiply(BigDecimal.valueOf(quantity));

      if (amount.compareTo(expected) < 0) {
        throw new IllegalStateException("Amount lower than cost price");
      }
    }

    StockContext stockContext = context.toStockContext();

    if (!stockService.exists(product.getId(), stockContext)) {
      stockService.initStock(product.getId(), stockContext, 0);
    }

    stockService.decrease(product.getId(), stockContext, quantity);

    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PRODUCT_SALE,
            paymentMethod,
            context,
            amount,
            null,
            comment,
            product.getId(),
            null,
            null,
            performedBy));
  }
}