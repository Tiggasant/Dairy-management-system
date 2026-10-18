package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByNameAsc();

    List<Customer> findByActiveTrueOrderByNameAsc();

    /** Row lock: serialises concurrent payments of the same customer. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
