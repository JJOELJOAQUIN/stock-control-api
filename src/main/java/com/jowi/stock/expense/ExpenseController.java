package com.jowi.stock.expense;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

  private final ExpenseService service;

  public ExpenseController(ExpenseService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest req) {
    Expense created = service.create(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseResponse.from(created));
  }

  @GetMapping
  public ResponseEntity<Page<ExpenseResponse>> list(
      @RequestParam(required = false) ExpenseContext context,
      Pageable pageable
  ) {
    Page<Expense> page = (context == null)
        ? service.list(pageable)
        : service.listByContext(context, pageable);

    return ResponseEntity.ok(page.map(ExpenseResponse::from));
  }
}
