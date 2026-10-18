package com.ghunsai.dairy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductPriceHistoryDTO(
        Long id,
        Long productId,
        BigDecimal sellingPrice,
        LocalDateTime effectiveFrom) {
}
