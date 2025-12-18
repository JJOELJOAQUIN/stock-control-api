package com.jowi.stock.movement;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

  private final StockMovementService stockMovementService;

  public StockMovementController(StockMovementService stockMovementService) {
    this.stockMovementService = stockMovementService;
  }

  @PostMapping("/in")
  public ResponseEntity<StockMovement> registerIn(
      @RequestParam UUID productId,
      @RequestParam int quantity,
      @RequestParam(required = false) String reason
  ) {
    StockMovement movement =
        stockMovementService.registerIn(productId, quantity, reason);

    return ResponseEntity.status(HttpStatus.CREATED).body(movement);
  }

  @PostMapping("/out")
  public ResponseEntity<StockMovement> registerOut(
      @RequestParam UUID productId,
      @RequestParam int quantity,
      @RequestParam(required = false) String reason
  ) {
    StockMovement movement =
        stockMovementService.registerOut(productId, quantity, reason);

    return ResponseEntity.status(HttpStatus.CREATED).body(movement);
  }

  @PostMapping("/adjust")
  public ResponseEntity<StockMovement> adjust(
      @RequestParam UUID productId,
      @RequestParam int quantity,
      @RequestParam String reason
  ) {
    StockMovement movement =
        stockMovementService.adjust(productId, quantity, reason);

    return ResponseEntity.status(HttpStatus.CREATED).body(movement);
  }
}
