package com.jowi.stock.dashboard;

public record DashboardSummaryResponse(
    long totalProducts,
    long productsWithStock,
    long productsWithoutStock,
    long lowStock,
    long totalMovements,
    long movementsToday
) {}
