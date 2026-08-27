package com.jowi.stock.cash.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.cash.enums.CashMovementType;
import com.jowi.stock.cash.enums.CashSource;
import com.jowi.stock.cash.enums.PaymentMethod;

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
    String procedureCode) {

  /**
   * Constructor de compatibilidad con los 12 componentes originales, para los
   * flujos que no registran procedimientos (ventas, compras, egresos).
   * Agregar un componente al record ya rompió cuatro call sites una vez.
   */
  public CreateCashMovementRequest(
      CashMovementType type, CashSource source, PaymentMethod paymentMethod,
      CashContext context, BigDecimal amount, BigDecimal retentionPercent,
      String comment, String detail, UUID referenceId,
      BigDecimal doctorSharePercent, BigDecimal cosmetologistSharePercent,
      CashActor performedBy) {
    this(type, source, paymentMethod, context, amount, retentionPercent,
        comment, detail, referenceId, doctorSharePercent,
        cosmetologistSharePercent, performedBy, null);
  }
}
