package com.jowi.stock.metrics.entities;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Efectivo que la cosmetóloga le deja a la médica.
 *
 * La cuenta del mes dice cuánto le corresponde a la médica de lo que hizo la
 * cosmetóloga (su 30% / 50% y el 95% de los productos). Cuando la
 * cosmetóloga deja efectivo, lo registra acá y se descuenta de ese saldo:
 * "Le dejás a la médica" − "Efectivo que dejaste" = lo que falta (o lo que
 * dejó de más).
 *
 * Es un registro aparte de la caja a propósito: no es un ingreso nuevo (la
 * plata ya entró como venta o procedimiento), es la rendición entre las dos.
 */
@Entity
@Table(name = "cosmetologist_cash_deposits", indexes = {
    @Index(name = "idx_cosmo_cash_date", columnList = "deposit_date")
})
public class CosmetologistCashDeposit extends BaseEntity {

  /** Día en que dejó el efectivo; define a qué mes de la cuenta suma. */
  @NotNull
  @Column(name = "deposit_date", nullable = false)
  private LocalDate depositDate;

  @NotNull
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(length = 300)
  private String note;

  /** Email de quien lo cargó. */
  @Column(name = "created_by", nullable = false, length = 200)
  private String createdBy;

  public LocalDate getDepositDate() {
    return depositDate;
  }

  public void setDepositDate(LocalDate depositDate) {
    this.depositDate = depositDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
}
