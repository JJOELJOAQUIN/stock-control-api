package com.jowi.stock.business.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.PaymentMethod;

/**
 * Sesión de tratamiento con recetario (BOM): un ingreso de caja más el
 * consumo de insumos, descontado en la unidad consumible de cada producto.
 *
 * El reparto viaja en el request porque el mismo flujo sirve para las dos:
 * los dermatológicos de Pili van 100/0 y los de Gise (Dermapen, Hydra,
 * Exosomas) van 30/70. Null en los percents = 100% médica, así el cliente
 * de ayer sigue andando.
 */
public record DermatoProcedureRequest(
    String procedureCode,
    String description,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    CashContext context,
    String comment,
    BigDecimal doctorSharePercent,
    BigDecimal cosmetologistSharePercent,
    CashActor performedBy,
    List<ConsumptionLine> consumptions) {

  public record ConsumptionLine(UUID productId, int quantity) {
  }
}