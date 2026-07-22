package com.jowi.stock.shopping.repositories;

import com.jowi.stock.cash.enums.CashContext;
import com.jowi.stock.shopping.entities.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ShoppingListRepository extends JpaRepository<ShoppingListItem, UUID> {
  List<ShoppingListItem> findByContextOrderByDoneAscCreatedAtDesc(CashContext context);
}