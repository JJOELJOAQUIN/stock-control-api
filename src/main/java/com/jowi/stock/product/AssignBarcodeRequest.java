package com.jowi.stock.product;

import jakarta.validation.constraints.NotBlank;

public record AssignBarcodeRequest(
    @NotBlank String barcode
) {}
