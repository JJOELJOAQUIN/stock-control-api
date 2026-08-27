package com.jowi.stock.product.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jowi.stock.product.entities.Product;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {
    long count();

    long countByActiveTrue();

    Optional <Product> findByBarcode(String barcode);
}
