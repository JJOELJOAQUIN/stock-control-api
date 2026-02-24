package com.jowi.stock.integration;

import com.jowi.stock.product.*;
import com.jowi.stock.stock.*;
import com.jowi.stock.movement.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class StockIntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    StockService stockService;

    @Autowired
    StockMovementRepository movementRepository;

    @Test
    void fullStockFlow_shouldWorkCorrectly() {

        // 1️⃣ Crear producto
        CreateProductRequest req = new CreateProductRequest(
                "IBUPROFENO",
                "Analgésico",
                5,
                ProductCategory.OTRO,
                ProductBrand.MAGISTRALES,
                true,
                ProductScope.BOTH,
                null);

        Product product = productService.create(req);

        UUID productId = product.getId();

        assertNotNull(productId);

        // 2️⃣ Init stock
        stockService.initStock(productId, StockContext.LOCAL, 10);

        Stock stock = stockService.getStock(productId, StockContext.LOCAL);

        assertEquals(10, stock.getCurrent());

        // 3️⃣ Increase
        stockService.increase(productId, StockContext.LOCAL, 5);

        stock = stockService.getStock(productId, StockContext.LOCAL);

        assertEquals(15, stock.getCurrent());

        // 4️⃣ Decrease
        stockService.decrease(productId, StockContext.LOCAL, 3);

        stock = stockService.getStock(productId, StockContext.LOCAL);

        assertEquals(12, stock.getCurrent());

        // 5️⃣ Verificar movimientos
        long movements = movementRepository.count();

        assertEquals(3, movements);
        // 1 adjust init
        // 1 increase
        // 1 decrease
    }

    @Test
    void decrease_moreThanStock_shouldThrow() {

       CreateProductRequest req = new CreateProductRequest(
                "paracetamol",
                "Analgésico",
                5,
                ProductCategory.OTRO,
                ProductBrand.MAGISTRALES,
                true,
                ProductScope.BOTH,
                null);
        Product product = productService.create(req);
        UUID id = product.getId();

        stockService.initStock(id, StockContext.LOCAL, 5);

        assertThrows(IllegalStateException.class, () -> stockService.decrease(id, StockContext.LOCAL, 10));
    }

    @Test
    void initTwice_shouldThrowConflict() {

        CreateProductRequest req = new CreateProductRequest(
                "curaplus",
                "Analgésico",
                5,
                ProductCategory.OTRO,
                ProductBrand.MAGISTRALES,
                true,
                ProductScope.BOTH,
                null);

        Product product = productService.create(req);
        UUID id = product.getId();

        stockService.initStock(id, StockContext.LOCAL, 5);

        assertThrows(IllegalStateException.class, () -> stockService.initStock(id, StockContext.LOCAL, 5));
    }
}