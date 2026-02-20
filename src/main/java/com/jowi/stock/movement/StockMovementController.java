package com.jowi.stock.movement;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jowi.stock.stock.StockContext;

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
      @RequestParam(required = false) OffsetDateTime from,
      @RequestParam(required = false) OffsetDateTime to,
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
