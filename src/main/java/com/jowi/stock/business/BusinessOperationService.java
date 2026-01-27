package com.jowi.stock.business;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jowi.stock.cash.*;
import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductService;
import com.jowi.stock.stock.StockService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class BusinessOperationService {

  private final StockService stockService;
  private final CashMovementService cashService;
  private final ProductService productService;

  public BusinessOperationService(
      StockService stockService,
      CashMovementService cashService,
      ProductService productService
  ) {
    this.stockService = stockService;
    this.cashService = cashService;
    this.productService = productService;
  }

  // =========================
  // VENTA DE PRODUCTO
  // =========================
  public void sellProduct(
      UUID productId,
      int quantity,
      BigDecimal amount,
      PaymentMethod paymentMethod,
      CashContext context,
      String comment
  ) {

    // 1️⃣ Validar producto
     productService.getById(productId);

    // 2️⃣ Egreso de stock (genera StockMovement)
    stockService.decrease(productId, quantity);

    // 3️⃣ Ingreso de dinero (genera CashMovement)
    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.IN,
            CashSource.PRODUCT_SALE,
            paymentMethod,
            context,
            amount,
            null,          // retentionPercent (usa default si es tarjeta)
            comment,
            productId      // referencia lógica
        )
    );
  }

  // =========================
  // COMPRA A PROVEEDOR
  // =========================
  public void purchaseProduct(
      UUID productId,
      int quantity,
      BigDecimal amount,
      String comment
  ) {

    // 1️⃣ Ingreso de stock
    stockService.increase(productId, quantity);

    // 2️⃣ Egreso de dinero
    cashService.create(
        new CreateCashMovementRequest(
            CashMovementType.OUT,
            CashSource.PROVIDER_PAYMENT,
            PaymentMethod.TRANSFER,
            CashContext.LOCAL,
            amount,
            BigDecimal.ZERO, // sin retención
            comment,
            productId
        )
    );
  }
}
