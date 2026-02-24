package com.jowi.stock.alert;

import com.jowi.stock.stock.StockContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)

class AlertControllerTest {

  @Autowired MockMvc mvc;

    @MockitoBean
    AlertService alertService;

  @Test
  void lowStock_ok() throws Exception {
    var resp = List.of(
        new AlertResponse(
            AlertType.LOW_STOCK,
            UUID.randomUUID(),
            "IBUPROFENO",
            "Stock por debajo del mínimo (1/5)",
            java.time.OffsetDateTime.now()
        )
    );

    when(alertService.lowStock(StockContext.LOCAL)).thenReturn(resp);

    mvc.perform(get("/api/alerts/low-stock")
            .param("context", "LOCAL")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].type").value("LOW_STOCK"))
        .andExpect(jsonPath("$[0].productName").value("IBUPROFENO"));
  }

  @Test
  void lowStock_missingContext_400() throws Exception {
    mvc.perform(get("/api/alerts/low-stock"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void lowStock_invalidContext_400() throws Exception {
    mvc.perform(get("/api/alerts/low-stock").param("context", "NO_EXISTE"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void outOfStock_ok() throws Exception {
    when(alertService.outOfStock(StockContext.CONSULTORIO)).thenReturn(List.of());

    mvc.perform(get("/api/alerts/out-of-stock").param("context", "CONSULTORIO"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }
}