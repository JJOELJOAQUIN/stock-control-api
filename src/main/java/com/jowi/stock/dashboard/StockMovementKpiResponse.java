package com.jowi.stock.dashboard;

public record StockMovementKpiResponse(
    long totalIn,
    long totalOut,
    long quantityIn,
    long quantityOut
) {}
