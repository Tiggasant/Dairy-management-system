package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.CustomerPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long customerId,
        String customerName,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String notes,
        String recordedBy) {

    public static PaymentResponse from(CustomerPayment p) {
        return new PaymentResponse(p.getId(), p.getCustomer().getId(), p.getCustomer().getName(),
                p.getAmount(), p.getPaymentDate(), p.getNotes(), p.getRecordedBy().getUsername());
    }
}
