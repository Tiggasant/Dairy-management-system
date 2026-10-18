package com.ghunsai.dairy.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveRequest(@NotNull(message = "active is required") Boolean active) {
}
