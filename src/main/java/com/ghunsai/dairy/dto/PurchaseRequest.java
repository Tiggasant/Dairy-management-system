package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PurchaseRequest(
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        @Digits(integer = 9, fraction = 3, message = "Quantity allows up to 3 decimals") BigDecimal quantity,
        @Size(max = 255, message = "Notes cannot exceed 255 characters") String notes) {
}
