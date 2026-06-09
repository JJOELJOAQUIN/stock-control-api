package com.jowi.stock.business.services;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.jowi.stock.batch.services.ProductBatchService;
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
import java.time.LocalDate;
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

  public void purchaseProduct(
      UUID productId,
      int quantity,
      BigDecimal amount,
      CashContext context,
      String comment,
      LocalDate expirationDate,
      String lotNumber,
      Boolean updateCostPrice,
      Boolean updateSalePrice,
      BigDecimal newSalePrice,
      Boolean updateMarkupPercentage,
      BigDecimal newDefaultMarkupPercentage) {

    StockContext stockContext = context.toStockContext();

    var product = productService.getById(productId);

    if (!stockService.exists(productId, stockContext)) {
      stockService.initStock(productId, stockContext, 0);
    }

    stockService.increase(productId, stockContext, quantity);

    batchService.createBatch(
        productId,
        stockContext,
        quantity,
        expirationDate,
        lotNumber);

    BigDecimal unitCost = amount.divide(
        BigDecimal.valueOf(quantity),
        2,
        java.math.RoundingMode.HALF_UP);

    if (Boolean.TRUE.equals(updateCostPrice)) {
      product.setCostPrice(unitCost);
    }

    if (Boolean.TRUE.equals(updateSalePrice) && newSalePrice != null) {
      product.setSalePrice(newSalePrice);
    }

    if (Boolean.TRUE.equals(updateMarkupPercentage)) {
      if (newDefaultMarkupPercentage == null) {
        throw new IllegalArgumentException("newDefaultMarkupPercentage is required");
      }

      product.setDefaultMarkupPercentage(newDefaultMarkupPercentage);
    }

    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.OUT,
            CashSource.PROVIDER_PAYMENT,
            PaymentMethod.TRANSFER,
            context,
            amount,
            BigDecimal.ZERO,
            comment,
            productId,
            null,
            null,
            null));
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