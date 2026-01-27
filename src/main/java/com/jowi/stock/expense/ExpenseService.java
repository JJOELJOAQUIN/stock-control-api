package com.jowi.stock.expense;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ExpenseService {

  private final ExpenseRepository repository;

  public ExpenseService(ExpenseRepository repository) {
    this.repository = repository;
  }

  public Expense create(CreateExpenseRequest req) {
    if (req == null) throw new IllegalArgumentException("request is required");
    if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amount must be > 0");
    }

    Expense e = new Expense();
    e.setType(req.type());
    e.setContext(req.context());
    e.setAmount(req.amount());
    e.setComment(req.comment());
    e.setRecurring(req.recurring() != null && req.recurring());

    return repository.save(e);
  }

  public Page<Expense> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  public Page<Expense> listByContext(ExpenseContext context, Pageable pageable) {
    if (context == null) throw new IllegalArgumentException("context is required");
    return repository.findByContext(context, pageable);
  }
}
