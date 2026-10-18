package com.ghunsai.dairy.dto;

import java.math.BigDecimal;

public record StockLevelResponse(Long productId, BigDecimal availableStock) {
}
