package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** Rows: [movementType, totalQuantity] for one product. */
    @Query("""
            SELECT m.movementType, SUM(m.quantity)
            FROM StockMovement m
            WHERE m.product.id = :productId
            GROUP BY m.movementType
            """)
    List<Object[]> sumByType(@Param("productId") Long productId);

    /** Rows: [productId, movementType, totalQuantity] for every product (stock report in one query). */
    @Query("""
            SELECT m.product.id, m.movementType, SUM(m.quantity)
            FROM StockMovement m
            GROUP BY m.product.id, m.movementType
            """)
    List<Object[]> sumByProductAndType();

    @EntityGraph(attributePaths = {"product", "createdBy"})
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"product", "createdBy"})
    Page<StockMovement> findAll(Pageable pageable);
}
