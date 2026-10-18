package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 100, message = "Product name cannot exceed 100 characters") String name,
        @NotBlank(message = "Category is required") @Size(max = 50) String category,
        @NotBlank(message = "Unit is required") @Size(max = 20) String unit,
        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Selling price allows up to 8 digits and 2 decimals")
        BigDecimal sellingPrice,
        @NotNull(message = "Low stock threshold is required")
        @DecimalMin(value = "0.000", message = "Low stock threshold cannot be negative")
        @Digits(integer = 9, fraction = 3, message = "Threshold allows up to 3 decimals") BigDecimal lowStockThreshold) {
}
