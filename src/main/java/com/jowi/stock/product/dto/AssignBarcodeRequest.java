package com.jowi.stock.product.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignBarcodeRequest(
    @NotBlank String barcode
) {}
