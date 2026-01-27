package com.jowi.stock.dashboard;

public record MovementKpiResponse(
    long totalMovements,

    long inCount,
    long outCount,
    long adjustCount,

    long todayMovements,
    long weekMovements,

    long totalInQuantity,
    long totalOutQuantity
) {}
