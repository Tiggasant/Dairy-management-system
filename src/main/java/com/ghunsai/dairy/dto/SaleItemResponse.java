package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.SaleItem;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long productId,
        String productName,
        String unit,
        BigDecimal quantity,
        BigDecimal sellingPrice,
        BigDecimal subtotal) {

    public static SaleItemResponse from(SaleItem i) {
        return new SaleItemResponse(i.getProduct().getId(), i.getProduct().getName(), i.getProduct().getUnit(),
                i.getQuantity(), i.getSellingPrice(), i.getSubtotal());
    }
}
