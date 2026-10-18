package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.CustomerPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long> {

    @EntityGraph(attributePaths = {"customer", "recordedBy"})
    List<CustomerPayment> findByCustomerIdOrderByPaymentDateDescIdDesc(Long customerId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM CustomerPayment p
            WHERE p.customer.id = :customerId
            """)
    BigDecimal getTotalPaidByCustomerId(@Param("customerId") Long customerId);

    /** Rows: [customerId, totalPaid]. */
    @Query("""
            SELECT p.customer.id, SUM(p.amount)
            FROM CustomerPayment p
            GROUP BY p.customer.id
            """)
    List<Object[]> totalPaidGroupedByCustomer();
}
