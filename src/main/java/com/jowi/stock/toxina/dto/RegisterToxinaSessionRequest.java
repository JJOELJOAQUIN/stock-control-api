package com.jowi.stock.toxina.dto;

import com.jowi.stock.cash.enums.CashContext;

import java.util.UUID;

/**
 * Registro de una sesión de toxina. productId = el vial de toxina (Xeomin).
 * unitsUsed = unidades que carga la doctora a mano (ej. 25). sessionNumber
 * = 1 o 2. context es opcional (default CONSULTORIO).
 */
public record RegisterToxinaSessionRequest(
    UUID productId,
    Integer sessionNumber,
    Integer unitsUsed,
    CashContext context
) {}