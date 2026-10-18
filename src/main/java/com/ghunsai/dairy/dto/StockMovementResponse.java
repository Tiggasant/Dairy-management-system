package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.MovementType;
import com.ghunsai.dairy.entity.StockMovement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        Long productId,
        String productName,
        MovementType movementType,
        BigDecimal quantity,
        LocalDateTime createdAt,
        String notes,
        String createdBy) {

    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(m.getId(), m.getProduct().getId(), m.getProduct().getName(),
                m.getMovementType(), m.getQuantity(), m.getCreatedAt(), m.getNotes(),
                m.getCreatedBy().getUsername());
    }
}
