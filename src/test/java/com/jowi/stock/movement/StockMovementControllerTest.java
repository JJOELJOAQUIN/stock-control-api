package com.jowi.stock.movement;

import com.jowi.stock.stock.StockContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockMovementController.class)
@AutoConfigureMockMvc(addFilters = false)

class StockMovementControllerTest {

  @Autowired
  MockMvc mvc;

  @MockitoBean
  StockMovementService service;

  @Test
  void search_ok_minimalParams() throws Exception {
    UUID productId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);

    Page<StockMovement> page = new PageImpl<>(List.of(), pageable, 0);

    when(service.search(
        eq(productId),
        eq(StockContext.LOCAL),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        any(Pageable.class))).thenReturn(page);

    mvc.perform(get("/api/stock-movements")
        .param("productId", productId.toString())
        .param("context", "LOCAL")
        .param("page", "0")
        .param("size", "20")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void search_missingProductId_400() throws Exception {
    mvc.perform(get("/api/stock-movements")
        .param("context", "LOCAL"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void search_withDates_ok() throws Exception {
    UUID productId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);

    OffsetDateTime from = OffsetDateTime.parse("2026-02-01T00:00:00-03:00");
    OffsetDateTime to = OffsetDateTime.parse("2026-02-23T23:59:59-03:00");

    when(service.search(
        eq(productId),
        eq(StockContext.CONSULTORIO),
        eq(StockMovementType.IN),
        eq(StockMovementReason.COMPRA_PROVEEDOR),
        eq(1),
        eq(100),
        eq(from),
        eq(to),
        any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

    mvc.perform(get("/api/stock-movements")
        .param("productId", productId.toString())
        .param("context", "CONSULTORIO")
        .param("type", "IN")
        .param("reason", "COMPRA_PROVEEDOR")
        .param("minQty", "1")
        .param("maxQty", "100")
        .param("from", from.toString())
        .param("to", to.toString())
        .param("page", "0")
        .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }
}