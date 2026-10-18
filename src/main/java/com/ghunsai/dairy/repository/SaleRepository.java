package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"customer", "createdBy"})
    Page<Sale> findBySaleDateGreaterThanEqualAndSaleDateLessThan(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "createdBy"})
    Optional<Sale> findWithDetailsById(Long id);

    Optional<Sale> findByCreatedByIdAndClientRequestId(Long createdById, String clientRequestId);

    /** Row: [count, total, cash, upi, udhaar] (sums are null when there are no sales). */
    @Query("""
            SELECT COUNT(s), SUM(s.totalAmount), SUM(s.cashAmount), SUM(s.upiAmount), SUM(s.udhaarAmount)
            FROM Sale s
            WHERE s.saleDate >= :start AND s.saleDate < :end
            """)
    List<Object[]> summarize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(s), SUM(s.totalAmount), SUM(s.cashAmount), SUM(s.upiAmount), SUM(s.udhaarAmount)
            FROM Sale s
            WHERE s.saleDate >= :start AND s.saleDate < :end AND s.createdBy.id = :userId
            """)
    List<Object[]> summarizeByUser(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("userId") Long userId);

    @Query("""
            SELECT COALESCE(SUM(s.udhaarAmount), 0)
            FROM Sale s
            WHERE s.customer.id = :customerId
            """)
    BigDecimal getTotalUdhaarByCustomerId(@Param("customerId") Long customerId);

    /** Rows: [customerId, totalUdhaar]. */
    @Query("""
            SELECT s.customer.id, SUM(s.udhaarAmount)
            FROM Sale s
            WHERE s.customer IS NOT NULL
            GROUP BY s.customer.id
            """)
    List<Object[]> totalUdhaarGroupedByCustomer();
}
