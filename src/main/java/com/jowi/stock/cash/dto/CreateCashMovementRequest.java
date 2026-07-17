package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;
import com.jowi.stock.cash.enums.PeelingPaymentKind;
import com.jowi.stock.cash.enums.SplitPreset;

/**
 * @param procedureCode     código del procedimiento (sólo source = PROCEDURE).
 *                          Antes el procedimiento sólo viajaba como texto en
 *                          detail/comment: sin un código estructurado no hay
 *                          forma de validar reglas por procedimiento sin
 *                          comparar strings de UI.
 * @param splitPreset       reparto del pago. Sólo admite valores distintos de
 *                          NORMAL en el peeling profundo. Null se trata como
 *                          NORMAL.
 * @param peelingPaymentKind cuota del peeling. Necesario para validar que
 *                          TODO_COSMETOLOGA sea efectivamente la primera.
 * @param performedBy       quién hizo el trabajo. Obligatorio en PRODUCT_SALE
 *                          de consultorio; en PROCEDURE se persiste en el ítem
 *                          para que la card de la cosmetóloga no dependa de
 *                          inferir la autoría desde el monto.
 */
public record CreateCashMovementRequest(
                CashMovementType type,
                CashSource source,
                PaymentMethod paymentMethod,
                CashContext context,
                BigDecimal amount,
                BigDecimal retentionPercent,
                String comment,
                String detail,
                UUID referenceId,
                BigDecimal doctorSharePercent,
                BigDecimal cosmetologistSharePercent,
                CashActor performedBy,
                String procedureCode,
                SplitPreset splitPreset,
                PeelingPaymentKind peelingPaymentKind) {
}