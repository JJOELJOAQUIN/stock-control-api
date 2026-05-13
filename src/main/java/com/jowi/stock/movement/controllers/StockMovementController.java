package com.jowi.stock.movement.controllers;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.movement.dto.StockMovementResponse;
import com.jowi.stock.movement.enums.StockMovementReason;
import com.jowi.stock.movement.enums.StockMovementType;
import com.jowi.stock.movement.services.StockMovementService;
import com.jowi.stock.stock.enums.StockContext;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

  private final StockMovementService service;

  public StockMovementController(StockMovementService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<Page<StockMovementResponse>> search(
      @RequestParam UUID productId,
      @RequestParam(required = false) StockMovementType type,
      @RequestParam(required = false) StockMovementReason reason,
      @RequestParam(required = false) Integer minQty,
      @RequestParam(required = false) Integer maxQty,
      @RequestParam(required = false) java.time.LocalDateTime from,
      @RequestParam(required = false) java.time.LocalDateTime to,
      @RequestParam StockContext context,
      Pageable pageable) {
    return ResponseEntity.ok(

        service.search(
            productId,
            context,
            type,
            reason,
            minQty,
            maxQty,
            from,
            to,
            pageable)

            .map(StockMovementResponse::from));
  }
}
