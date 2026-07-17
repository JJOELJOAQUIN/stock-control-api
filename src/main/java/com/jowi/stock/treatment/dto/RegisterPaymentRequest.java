package com.jowi.stock.treatment.dto;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.enums.SplitPreset;
import java.math.BigDecimal;

/**
 * @param splitPreset reparto del pago. Null se trata como NORMAL. Sólo admite
 *                    valores distintos en el peeling profundo, y
 *                    TODO_COSMETOLOGA sólo en la primera cuota. La cuota no
 *                    viaja: la calcula el backend desde los pagos existentes.
 */
public record RegisterPaymentRequest(
    BigDecimal amount,
    PaymentMethod paymentMethod,
    CashContext context,
    SplitPreset splitPreset) {
}