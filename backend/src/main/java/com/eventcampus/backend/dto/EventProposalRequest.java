package com.eventcampus.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventProposalRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 15000) String description,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @NotNull LocalTime eventTime,
        @NotBlank @Size(max = 255) String location,
        @NotNull @Positive Integer capacity,
        @NotNull @Positive Integer idCategory) {
}
