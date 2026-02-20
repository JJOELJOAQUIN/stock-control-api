package com.jowi.stock.product;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {
    long count();

    long countByActiveTrue();
    
    Optional <Product> findByBarcode(String barcode);
}
