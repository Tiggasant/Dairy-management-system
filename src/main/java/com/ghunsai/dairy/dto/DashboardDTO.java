package com.ghunsai.dairy.dto;

import java.math.BigDecimal;

public record DashboardDTO(
        BigDecimal todaySales,
        BigDecimal cashCollection,
        BigDecimal upiCollection,
        BigDecimal todayUdhaar,
        long totalBills) {
}
