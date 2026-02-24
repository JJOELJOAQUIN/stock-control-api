package com.jowi.stock.cash;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CashMovementController.class)
@AutoConfigureMockMvc(addFilters = false)

class CashMovementControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    CashMovementService service;

    @Test
    void create_201() throws Exception {

        var req = new CreateCashMovementRequest(
                CashMovementType.IN,
                CashSource.PRODUCT_SALE,
                PaymentMethod.CASH,
                CashContext.LOCAL,
                new BigDecimal("1000.00"),
                null,
                "venta mostrador",
                UUID.randomUUID());

        CashMovement created = new CashMovement();
        created.setType(req.type());
        created.setSource(req.source());
        created.setPaymentMethod(req.paymentMethod());
        created.setContext(req.context());
        created.setAmount(req.amount());
        created.setRetention(BigDecimal.ZERO);
        created.setNetAmount(req.amount());
        created.setComment(req.comment());
        created.setReferenceId(req.referenceId());

        when(service.create(any(CreateCashMovementRequest.class)))
                .thenReturn(created);

        mvc.perform(post("/api/cash-movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IN"))
                .andExpect(jsonPath("$.source").value("PRODUCT_SALE"))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    void list_noContext_ok() throws Exception {

        Pageable pageable = PageRequest.of(0, 20);

        Page<CashMovement> page = new PageImpl<>(List.of(), pageable, 0);

        when(service.list(any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/cash-movements")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void list_withContext_ok() throws Exception {

        Pageable pageable = PageRequest.of(0, 10);

        Page<CashMovement> page = new PageImpl<>(List.of(), pageable, 0);

        when(service.listByContext(eq(CashContext.LOCAL), any(Pageable.class)))
                .thenReturn(page);

        mvc.perform(get("/api/cash-movements")
                .param("context", "LOCAL")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}