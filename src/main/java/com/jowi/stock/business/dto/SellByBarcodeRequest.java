package com.jowi.stock.business.dto;

import java.math.BigDecimal;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SellByBarcodeRequest(

        @NotBlank String barcode,

        @NotNull @Min(1) Integer quantity,

        @NotNull BigDecimal amount,

        @NotNull PaymentMethod paymentMethod,

        @NotNull CashContext context,

        String comment,

        @NotNull CashActor performedBy
 ) {
}
