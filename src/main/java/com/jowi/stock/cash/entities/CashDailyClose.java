package com.jowi.stock.cash.entities;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Snapshot del cierre de caja de un día y contexto: cuánto entró/salió por
 * método (efectivo, transferencia, débito, crédito) y quién lo cerró.
 *
 * Es un registro histórico para conciliar: "al cierre del 12/08 había tanto en
 * efectivo y tanto en transferencia". No bloquea movimientos posteriores (eso
 * queda como mejora futura); es la foto de los totales al momento de cerrar.
 *
 * Único por (context, close_date): un día se cierra una sola vez por contexto.
 */
@Entity
@Table(
    name = "cash_daily_close",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cash_daily_close_ctx_date", columnNames = {"context", "close_date"}))
public class CashDailyClose extends BaseEntity {

  @Column(name = "close_date", nullable = false)
  private LocalDate closeDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CashContext context;

  // Neto por método (IN - OUT) al momento del cierre.
  @Column(name = "cash_net", nullable = false, precision = 18, scale = 2)
  private BigDecimal cashNet = BigDecimal.ZERO;

  @Column(name = "transfer_net", nullable = false, precision = 18, scale = 2)
  private BigDecimal transferNet = BigDecimal.ZERO;

  @Column(name = "debit_net", nullable = false, precision = 18, scale = 2)
  private BigDecimal debitNet = BigDecimal.ZERO;

  @Column(name = "credit_net", nullable = false, precision = 18, scale = 2)
  private BigDecimal creditNet = BigDecimal.ZERO;

  @Column(name = "total_in", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalIn = BigDecimal.ZERO;

  @Column(name = "total_out", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalOut = BigDecimal.ZERO;

  @Column(name = "net_total", nullable = false, precision = 18, scale = 2)
  private BigDecimal netTotal = BigDecimal.ZERO;

  @Column(name = "closed_by", nullable = false, length = 120)
  private String closedBy;

  @Column(length = 300)
  private String note;

  public LocalDate getCloseDate() { return closeDate; }
  public void setCloseDate(LocalDate closeDate) { this.closeDate = closeDate; }

  public CashContext getContext() { return context; }
  public void setContext(CashContext context) { this.context = context; }

  public BigDecimal getCashNet() { return cashNet; }
  public void setCashNet(BigDecimal v) { this.cashNet = v; }

  public BigDecimal getTransferNet() { return transferNet; }
  public void setTransferNet(BigDecimal v) { this.transferNet = v; }

  public BigDecimal getDebitNet() { return debitNet; }
  public void setDebitNet(BigDecimal v) { this.debitNet = v; }

  public BigDecimal getCreditNet() { return creditNet; }
  public void setCreditNet(BigDecimal v) { this.creditNet = v; }

  public BigDecimal getTotalIn() { return totalIn; }
  public void setTotalIn(BigDecimal v) { this.totalIn = v; }

  public BigDecimal getTotalOut() { return totalOut; }
  public void setTotalOut(BigDecimal v) { this.totalOut = v; }

  public BigDecimal getNetTotal() { return netTotal; }
  public void setNetTotal(BigDecimal v) { this.netTotal = v; }

  public String getClosedBy() { return closedBy; }
  public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

  public String getNote() { return note; }
  public void setNote(String note) { this.note = note; }
}