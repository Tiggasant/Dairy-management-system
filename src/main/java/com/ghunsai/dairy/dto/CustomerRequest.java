package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Customer name is required") @Size(max = 100) String name,
        @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Phone must be 7-15 digits (+, - and space allowed)")
        String phone) {
}
