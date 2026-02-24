package com.jowi.stock.business;

import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jowi.stock.cash.CashContext;
import com.jowi.stock.cash.PaymentMethod;

@WebMvcTest(BusinessOperationController.class)
@AutoConfigureMockMvc(addFilters = false)

class BusinessOperationControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    BusinessOperationService service;

    @Test
    void sell_201() throws Exception {

        var req = new SellProductRequest(
                UUID.randomUUID(),
                2,
                new BigDecimal("1500.00"),
                PaymentMethod.CASH,
                CashContext.LOCAL,
                "venta"
        );

        mvc.perform(post("/api/business/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void sell_invalidBody_400() throws Exception {

        mvc.perform(post("/api/business/sell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellByBarcode_ok() throws Exception {

        var req = new SellByBarcodeRequest(
                "7791234567890",
                1,
                new BigDecimal("800.00"),
                PaymentMethod.DEBIT,
                CashContext.LOCAL,
                "venta"
        );

        mvc.perform(post("/api/business/sell-by-barcode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}