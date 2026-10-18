package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Amount allows up to 2 decimals") BigDecimal amount,
        @Size(max = 250, message = "Notes cannot exceed 250 characters") String notes) {
}
