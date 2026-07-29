package com.jowi.stock.procedure.entities;

import com.jowi.stock.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Un renglón de la receta (BOM) de un procedimiento: qué insumo consume y
 * cuánto, en la unidad consumible del producto.
 *
 * La cantidad es un entero en la unidad del producto. Para insumos que se
 * fraccionan (ej. NCTF, modelado en décimas de ml), 1,5ml se guarda como 15.
 * El motor de stock no cambia: descuenta enteros, como siempre.
 *
 * Esta tabla ES la base de "que la Dra cargue tratamientos sola": editarla
 * (o exponer un CRUD sobre ella) cambia el consumo sin tocar código.
 *
 * Una fila por (procedureCode, productId): un insumo no se repite en la misma
 * receta; si hace falta más cantidad, se sube el quantity.
 */
@Entity
@Table(name = "procedure_consumption",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_procedure_consumption",
        columnNames = {"procedure_code", "product_id"}),
    indexes = @Index(name = "idx_proc_consumption_code", columnList = "procedure_code"))
public class ProcedureConsumption extends BaseEntity {

  @NotNull
  @Column(name = "procedure_code", nullable = false, length = 60)
  private String procedureCode;

  @NotNull
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @NotNull
  @Min(1)
  @Column(nullable = false)
  private Integer quantity;

  public String getProcedureCode() {
    return procedureCode;
  }

  public void setProcedureCode(String procedureCode) {
    this.procedureCode = procedureCode;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }
}