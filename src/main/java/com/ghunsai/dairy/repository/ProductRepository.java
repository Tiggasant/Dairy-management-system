package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByActiveTrueOrderByNameAsc();

    Optional<Product> findByIdAndActiveTrue(Long id);

    /** Row lock: serialises concurrent sales / stock changes of the same product. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.active = true")
    Optional<Product> findActiveProductForUpdate(@Param("id") Long id);
}
