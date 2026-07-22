package com.jowi.stock.shopping.dto;

import com.jowi.stock.cash.enums.CashContext;
import java.util.UUID;

public record CreateShoppingItemRequest(
    String description,
    String note,
    CashContext context,
    UUID productId) {
}