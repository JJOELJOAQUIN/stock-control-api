package com.jowi.stock.product.dto;

import java.math.BigDecimal;
import java.util.List;

import com.jowi.stock.product.enums.ConsumptionUnit;
import com.jowi.stock.product.enums.ProductBrand;
import com.jowi.stock.product.enums.ProductCategory;
import com.jowi.stock.product.enums.ProductScope;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Integer minimumStock,
        @NotNull ProductCategory category,
        @NotNull ProductBrand brand,
        Boolean expirable,
        @NotNull ProductScope scope,
        String barcode,
        @NotNull BigDecimal costPrice,
        BigDecimal salePrice,
        BigDecimal defaultMarkupPercentage,
        @Min(1) Integer shelfLifeMonths,
        @Min(0) Integer restockPriority,

        // --- Unidad en la que se cuenta el stock del producto (ML/AMPOLLA/
        //     DISPARO/UNIDAD). Null = UNIDAD (retail, comportamiento de siempre). ---
        ConsumptionUnit consumptionUnit,

        // --- Cuántas unidades consumibles trae un envase comercial. La compra se
        //     carga en ENVASES y el stock ingresa multiplicado (caja NCTF 5x3ml
        //     -> 15). Null o 1 = envase y unidad coinciden (retail). ---
        @Min(1) Integer unitsPerPackage,

        // --- Asociación OPCIONAL a procedimientos (receta / BOM). Si viene vacía
        //     o null, el producto se crea suelto, como hasta ahora. ---
        List<ProductRecipeLineRequest> recipes) {

  /**
   * Un renglón de receta cargado desde la creación del producto: a qué
   * procedimiento se asocia este producto y cuánto se consume por sesión.
   * La cantidad es un entero en la unidad consumible del producto (con la
   * convención de x10 para fracciones, ej. 1,5 ml -> 15).
   */
  public record ProductRecipeLineRequest(
          @NotBlank String procedureCode,
          @NotNull @Min(1) Integer quantity) {
  }
}
