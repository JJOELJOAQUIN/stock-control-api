package com.jowi.stock.toxina.entities;

import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.product.entities.Product;
import com.jowi.stock.toxina.enums.OpenVialStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Un vial de toxina que fue abierto. Es la unidad central del flujo:
 * arranca con {@code totalUnits} (100 para Xeomin) y un reloj de 20 días
 * ({@code expiresAt}). Se comparte entre pacientes: cada sesión le descuenta
 * unidades hasta agotarlo o hasta que vence.
 *
 * <p>Al abrir un vial se descuenta 1 unidad del stock del producto; las
 * unidades internas se administran acá, no en el stock principal.
 */
@Entity
@Table(name = "open_vials", indexes = {
    @Index(name = "idx_open_vial_status", columnList = "status"),
    @Index(name = "idx_open_vial_product", columnList = "product_id")
})
public class OpenVial extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @NotNull
  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  // opened_at + 20 días. Se calcula al abrir el vial.
  @NotNull
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @NotNull
  @Column(name = "total_units", nullable = false)
  private Integer totalUnits = 100;

  @NotNull
  @Column(name = "units_remaining", nullable = false)
  private Integer unitsRemaining;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OpenVialStatus status = OpenVialStatus.OPEN;

  // ===== getters / setters =====

  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }

  public Instant getOpenedAt() { return openedAt; }
  public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

  public Integer getTotalUnits() { return totalUnits; }
  public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }

  public Integer getUnitsRemaining() { return unitsRemaining; }
  public void setUnitsRemaining(Integer unitsRemaining) { this.unitsRemaining = unitsRemaining; }

  public OpenVialStatus getStatus() { return status; }
  public void setStatus(OpenVialStatus status) { this.status = status; }
}
