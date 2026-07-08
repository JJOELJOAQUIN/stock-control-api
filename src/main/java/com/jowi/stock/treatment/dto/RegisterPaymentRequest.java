package com.jowi.stock.treatment.dto;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;
import java.math.BigDecimal;

public record RegisterPaymentRequest(
    BigDecimal amount,
    PaymentMethod paymentMethod,
    CashContext context) {
}