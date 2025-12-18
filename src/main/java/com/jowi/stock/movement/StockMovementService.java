package com.jowi.stock.movement;

import com.jowi.stock.product.Product;
import com.jowi.stock.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public StockMovementService(
            StockMovementRepository movementRepository,
            ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    public StockMovement registerIn(
            UUID productId,
            int quantity,
            String reason) {
        return createMovement(productId, StockMovementType.IN, quantity, reason);
    }

    public StockMovement registerOut(
            UUID productId,
            int quantity,
            String reason) {
        return createMovement(productId, StockMovementType.OUT, quantity, reason);
    }

    public StockMovement adjust(
            UUID productId,
            int quantity,
            String reason) {
        return createMovement(productId, StockMovementType.ADJUST, quantity, reason);
    }

    private int getCurrentStock(UUID productId) {
        Integer stock = movementRepository.calculateCurrentStock(productId);
        return stock != null ? stock : 0;
    }

    private StockMovement createMovement(
            UUID productId,
            StockMovementType type,
            int quantity,
            String reason) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        int currentStock = getCurrentStock(productId);

        if (type == StockMovementType.OUT && quantity > currentStock) {
            throw new IllegalStateException(
                    "Insufficient stock. Current: " + currentStock);
        }

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReason(reason);

        return movementRepository.save(movement);
    }

}
