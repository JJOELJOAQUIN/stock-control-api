package com.jowi.stock.integration;

import com.jowi.stock.TestSecurityConfig;
import com.jowi.stock.business.BusinessOperationService;
import com.jowi.stock.cash.*;
import com.jowi.stock.product.*;
import com.jowi.stock.stock.*;
import com.jowi.stock.movement.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")

@Transactional
class SellFlowIntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    StockService stockService;

    @Autowired
    BusinessOperationService businessService;

    @Autowired
    CashMovementRepository cashRepository;

    @Autowired
    StockMovementRepository movementRepository;

    @Test
    void sellProduct_shouldDecreaseStock_andCreateCashMovement() {

        // 1️⃣ Crear producto válido
        CreateProductRequest req = new CreateProductRequest(
                "CREMA FACIAL",
                "Antiage",
                5,
                ProductCategory.COSMETICO_VENTA,
                ProductBrand.GENERICO,
                true,
                ProductScope.BOTH,
                null
        );

        Product product = productService.create(req);
        UUID productId = product.getId();

        assertNotNull(productId);

        // 2️⃣ Inicializar stock con 10 unidades
        stockService.initStock(productId, StockContext.LOCAL, 10);

        Stock stock = stockService.getStock(productId, StockContext.LOCAL);
        assertEquals(10, stock.getCurrent());

        // 3️⃣ Ejecutar venta de 3 unidades
        businessService.sellProduct(
                productId,
                3,
                new BigDecimal("3000.00"),
                PaymentMethod.CASH,
                CashContext.LOCAL,
                "Venta mostrador"
        );

        // 4️⃣ Verificar stock decrementado
        stock = stockService.getStock(productId, StockContext.LOCAL);
        assertEquals(7, stock.getCurrent());

        // 5️⃣ Verificar movimientos de stock
        long stockMovements = movementRepository.count();
        assertEquals(2, stockMovements);
        // 1 INIT ADJUST
        // 1 OUT venta

        // 6️⃣ Verificar movimiento de caja
        long cashMovements = cashRepository.count();
        assertEquals(1, cashMovements);

        CashMovement movement = cashRepository.findAll().get(0);

        assertEquals(CashMovementType.IN, movement.getType());
        assertEquals(CashSource.PRODUCT_SALE, movement.getSource());
        assertEquals(new BigDecimal("3000.00"), movement.getAmount());
        assertEquals(CashContext.LOCAL, movement.getContext());
    }
}