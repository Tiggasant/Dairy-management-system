package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String category,
        String unit,
        BigDecimal sellingPrice,
        boolean active,
        BigDecimal lowStockThreshold) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getUnit(),
                p.getSellingPrice(), p.isActive(), p.getLowStockThreshold());
    }
}
