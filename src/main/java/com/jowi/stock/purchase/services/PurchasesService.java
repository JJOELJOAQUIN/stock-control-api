package com.jowi.stock.purchase.services;

import com.jowi.stock.auth.CurrentUserService;
import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.purchase.dto.MonthlyPurchasesResponse;
import com.jowi.stock.purchase.dto.MonthlyPurchasesResponse.PurchaseItemRow;
import com.jowi.stock.purchase.dto.MonthlyPurchasesResponse.PurchaseOrderRow;
import com.jowi.stock.purchase.repositories.PurchaseItemRepository;
import com.jowi.stock.common.BusinessZone;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchasesService {

  private final PurchaseItemRepository repository;
  private final CurrentUserService currentUserService;

  public PurchasesService(
      PurchaseItemRepository repository,
      CurrentUserService currentUserService) {
    this.repository = repository;
    this.currentUserService = currentUserService;
  }

  public MonthlyPurchasesResponse monthly(CashContext context, int year, int month) {
    if (context == null) throw new IllegalArgumentException("context is required");
    if (month < 1 || month > 12) throw new IllegalArgumentException("month must be 1..12");

    // Las compras son de Pili: la cosmetóloga no ve el gasto en mercadería.
    if (currentUserService.isCosmetologist()) {
      return new MonthlyPurchasesResponse(year, month, context, BigDecimal.ZERO, List.of());
    }

    // Mes delimitado en hora Argentina (BusinessZone), no en la zona de
    // la JVM: en Railway systemDefault() era UTC y corría el borde del mes.
    BusinessZone.Range range = BusinessZone.ofMonth(year, month);
    Instant from = range.from();
    Instant to = range.to();

    // Agrupo los ítems por orden preservando el orden de llegada (fecha desc).
    Map<String, OrderAccumulator> byOrder = new LinkedHashMap<>();

    for (Object[] r : repository.purchasesOfMonth(context, from, to)) {
      String cashId = r[0].toString();
      OrderAccumulator acc = byOrder.computeIfAbsent(cashId, k ->
          new OrderAccumulator(
              cashId,
              (Instant) r[1],
              // paymentMethod es un enum (PaymentMethod), no String:
              // @Enumerated(STRING) afecta el guardado, no el tipo que
              // devuelve JPQL. Castearlo a String tiraba ClassCastException
              // (500) apenas la query traía filas. toString() = su nombre.
              r[2] == null ? null : r[2].toString(),
              (String) r[3]));

      BigDecimal subtotal = dec(r[7]);
      acc.items.add(new PurchaseItemRow(
          (String) r[4],
          ((Number) r[5]).intValue(),
          dec(r[6]),
          subtotal,
          (String) r[8],
          (LocalDate) r[9]));
      acc.total = acc.total.add(subtotal);
    }

    List<PurchaseOrderRow> orders = new ArrayList<>();
    BigDecimal totalSpent = BigDecimal.ZERO;
    for (OrderAccumulator acc : byOrder.values()) {
      orders.add(new PurchaseOrderRow(
          acc.cashId, acc.date, acc.paymentMethod, acc.comment, acc.total, acc.items));
      totalSpent = totalSpent.add(acc.total);
    }

    return new MonthlyPurchasesResponse(year, month, context, totalSpent, orders);
  }

  private BigDecimal dec(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal b) return b;
    return BigDecimal.valueOf(((Number) value).doubleValue());
  }

  private static final class OrderAccumulator {
    final String cashId;
    final Instant date;
    final String paymentMethod;
    final String comment;
    final List<PurchaseItemRow> items = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;

    OrderAccumulator(String cashId, Instant date, String paymentMethod, String comment) {
      this.cashId = cashId;
      this.date = date;
      this.paymentMethod = paymentMethod;
      this.comment = comment;
    }
  }
}
