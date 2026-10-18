package com.ghunsai.dairy.repository;

import com.ghunsai.dairy.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    List<ProductPriceHistory> findByProductIdOrderByEffectiveFromDescIdDesc(Long productId);
}
