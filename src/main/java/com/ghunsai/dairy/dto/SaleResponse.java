package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.Sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bill header (used in lists). */
public record SaleResponse(
        Long id,
        LocalDateTime saleDate,
        BigDecimal totalAmount,
        BigDecimal cashAmount,
        BigDecimal upiAmount,
        BigDecimal udhaarAmount,
        Long customerId,
        String customerName,
        String createdBy) {

    public static SaleResponse from(Sale s) {
        return new SaleResponse(
                s.getId(), s.getSaleDate(), s.getTotalAmount(), s.getCashAmount(), s.getUpiAmount(),
                s.getUdhaarAmount(),
                s.getCustomer() != null ? s.getCustomer().getId() : null,
                s.getCustomer() != null ? s.getCustomer().getName() : null,
                s.getCreatedBy().getUsername());
    }
}
