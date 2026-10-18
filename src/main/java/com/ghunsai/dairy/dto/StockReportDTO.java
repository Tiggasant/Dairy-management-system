package com.ghunsai.dairy.dto;

import java.math.BigDecimal;

public record StockReportDTO(
        Long productId,
        String productName,
        String unit,
        BigDecimal availableStock,
        BigDecimal lowStockThreshold,
        boolean lowStock) {
}
