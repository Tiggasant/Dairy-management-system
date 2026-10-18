package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.SaleItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @EntityGraph(attributePaths = "product")
    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);
}
