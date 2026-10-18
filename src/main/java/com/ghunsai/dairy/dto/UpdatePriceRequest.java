package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePriceRequest(
        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Selling price allows up to 8 digits and 2 decimals")
        BigDecimal sellingPrice) {
}
