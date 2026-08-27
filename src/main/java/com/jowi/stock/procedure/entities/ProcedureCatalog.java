package com.jowi.stock.procedure.entities;

import com.jowi.stock.cash.enums.CashActor;
import com.jowi.stock.common.BaseEntity;
import com.jowi.stock.procedure.enums.ProcedureKind;
import com.jowi.stock.procedure.enums.ProcedureSpecialFlow;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Catálogo de tratamientos que ofrece el consultorio. Es la fuente de verdad
 * del reparto: cuando la Dra da de alta un tratamiento acá, el backend sabe
 * cómo repartir su plata sin tocar código.
 *
 * El reparto se guarda desnormalizado (performer + doctorPercent +
 * cosmetologistPercent) porque es lo que consumen los resolvers de caja, pero
 * SIEMPRE se setea desde una ProcedureSplitRule (ver ProcedureCatalogService):
 * no se cargan porcentajes sueltos.
 *
 * `code` es la clave de agregación de todo el sistema (métricas, BOM, caja).
 * Se guarda en MAYÚSCULAS y es único: renombrar un tratamiento cambia el
 * label, nunca el code.
 */
@Entity
@Table(
    name = "procedure_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_procedure_catalog_code", columnNames = "code"),
    indexes = @Index(name = "idx_procedure_catalog_active", columnList = "active"))
public class ProcedureCatalog extends BaseEntity {

  @Column(nullable = false, length = 60)
  private String code;

  @Column(nullable = false, length = 200)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProcedureKind kind;

  /** Quién hace el trabajo (define en qué card de métricas cae). */
  @Enumerated(EnumType.STRING)
  @Column(name = "performed_by", nullable = false, length = 20)
  private CashActor performer;

  @Column(name = "doctor_percent", nullable = false, precision = 5, scale = 4)
  private BigDecimal doctorPercent;

  @Column(name = "cosmetologist_percent", nullable = false, precision = 5, scale = 4)
  private BigDecimal cosmetologistPercent;

  /** Precio de lista. null o 0 = "a convenir" (se carga a mano en caja). */
  @Column(precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private boolean active = true;

  /**
   * Cómo se consume el insumo: receta fija (NONE) o flujo especial de vial
   * (TOXINA_VIAL). Los tratamientos NONE usan procedure_consumption.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "special_flow", nullable = false, length = 20)
  private ProcedureSpecialFlow specialFlow = ProcedureSpecialFlow.NONE;

  public ProcedureSpecialFlow getSpecialFlow() {
    return specialFlow;
  }

  public void setSpecialFlow(ProcedureSpecialFlow specialFlow) {
    this.specialFlow = specialFlow;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public ProcedureKind getKind() {
    return kind;
  }

  public void setKind(ProcedureKind kind) {
    this.kind = kind;
  }

  public CashActor getPerformer() {
    return performer;
  }

  public void setPerformer(CashActor performer) {
    this.performer = performer;
  }

  public BigDecimal getDoctorPercent() {
    return doctorPercent;
  }

  public void setDoctorPercent(BigDecimal doctorPercent) {
    this.doctorPercent = doctorPercent;
  }

  public BigDecimal getCosmetologistPercent() {
    return cosmetologistPercent;
  }

  public void setCosmetologistPercent(BigDecimal cosmetologistPercent) {
    this.cosmetologistPercent = cosmetologistPercent;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
