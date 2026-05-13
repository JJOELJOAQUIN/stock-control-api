package com.jowi.stock.expense.repositories;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jowi.stock.expense.entities.Expense;
import com.jowi.stock.expense.enums.ExpenseContext;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
  Page<Expense> findByContext(ExpenseContext context, Pageable pageable);
}
