package com.jowi.stock.stock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jowi.stock.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
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
    Product product = new Product();
    product.setName("Integration Test Product");
    product.setDescription("integration");
    product.setMinimumStock(5);

    String productResponse =
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Product createdProduct =
        objectMapper.readValue(productResponse, Product.class);

    // =====================
    // 2. INIT STOCK
    // =====================
    mockMvc.perform(post("/api/stock/" + createdProduct.getId() + "/init")
            .param("initialStock", "10"))
        .andExpect(status().isCreated());

    // =====================
    // 3. STOCK OUT
    // =====================
    mockMvc.perform(post("/api/stock/" + createdProduct.getId() + "/out")
            .param("qty", "3"))
        .andExpect(status().isOk());

    // =====================
    // 4. GET STOCK
    // =====================
    mockMvc.perform(get("/api/stock/" + createdProduct.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.current").value(7))
        .andExpect(jsonPath("$.belowMinimum").value(false));
  }
}