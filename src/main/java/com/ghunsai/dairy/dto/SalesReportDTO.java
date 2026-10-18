package com.ghunsai.dairy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportDTO(
        LocalDate date,
        long totalBills,
        BigDecimal totalSales,
        BigDecimal cashCollection,
        BigDecimal upiCollection,
        BigDecimal udhaarAmount) {
}
