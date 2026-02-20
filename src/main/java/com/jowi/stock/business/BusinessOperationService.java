package com.jowi.stock.business;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jowi.stock.cash.*;

import com.jowi.stock.product.ProductService;
import com.jowi.stock.stock.StockContext;
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
                        ProductService productService) {
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
                        String comment) {

                productService.getById(productId);

                // 🔧 AHORA PASA CONTEXTO
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
                                                productId));
        }

        // =========================
        // COMPRA A PROVEEDOR
        // =========================
        public void purchaseProduct(
                        UUID productId,
                        int quantity,
                        BigDecimal amount,
                        CashContext context,
                        String comment) {

                StockContext stockContext = context.toStockContext();

                // Ingreso stock
                stockService.increase(productId, stockContext, quantity);

                // Egreso dinero
                cashService.create(
                                new CreateCashMovementRequest(
                                                CashMovementType.OUT,
                                                CashSource.PROVIDER_PAYMENT,
                                                PaymentMethod.TRANSFER,
                                                context,
                                                amount,
                                                BigDecimal.ZERO,
                                                comment,
                                                productId));
        }

        public void sellByBarcode(
                        String barcode,
                        int quantity,
                        BigDecimal amount,
                        PaymentMethod paymentMethod,
                        CashContext context,
                        String comment) {

                var product = productService.getByBarcode(barcode);

                if (product.getCostPrice() != null) {

                        BigDecimal expected = product.getCostPrice()
                                        .multiply(BigDecimal.valueOf(quantity));

                        if (amount.compareTo(expected) < 0) {
                                throw new IllegalStateException("Amount lower than cost price");
                        }
                }

                StockContext stockContext = context.toStockContext();

                // 🔥 Evitamos try/catch como lógica de flujo
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
                                                product.getId()));
        }
}
