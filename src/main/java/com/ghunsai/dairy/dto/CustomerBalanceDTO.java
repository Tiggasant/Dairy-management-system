package com.ghunsai.dairy.dto;

import java.math.BigDecimal;

public record CustomerBalanceDTO(
        Long customerId,
        String customerName,
        BigDecimal totalUdhaar,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance) {
}
