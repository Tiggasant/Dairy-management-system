package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaleItemRequest(
        @NotNull(message = "Product ID is required") Long productId,
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        @Digits(integer = 9, fraction = 3, message = "Quantity allows up to 3 decimals") BigDecimal quantity) {
}
