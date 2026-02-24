package com.jowi.stock.stock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jowi.stock.product.CreateProductRequest;
import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductBrand;
import com.jowi.stock.product.ProductCategory;
import com.jowi.stock.product.ProductScope;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)

class StockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullStockFlowShouldWork() throws Exception {

        // =====================
        // 1. CREATE PRODUCT
        // =====================
        CreateProductRequest req = new CreateProductRequest(
                "Integration Test Product",
                "integration",
                5,
                ProductCategory.OTRO,
                ProductBrand.GENERICO,
                true,
                ProductScope.BOTH,

                null);

        String productResponse = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Product createdProduct = objectMapper.readValue(productResponse, Product.class);

        // =====================
        // 2. INIT STOCK
        // =====================
        mockMvc.perform(post("/api/stock/" + createdProduct.getId() + "/init")
                .param("initialStock", "10")
                .param("context", "LOCAL"))
                .andExpect(status().isCreated());

        // =====================
        // 3. STOCK OUT
        // =====================
        mockMvc.perform(post("/api/stock/" + createdProduct.getId() + "/out")
                .param("qty", "3")
                .param("context", "LOCAL"))
                .andExpect(status().isOk());

        // =====================
        // 4. GET STOCK
        // =====================
        mockMvc.perform(get("/api/stock/" + createdProduct.getId())
                .param("context", "LOCAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(7))
                .andExpect(jsonPath("$.belowMinimum").value(false));
    }
}