package com.eventcampus.backend.dto;

import jakarta.validation.constraints.*;

public record EventReportRequest(
        @NotBlank @Pattern(regexp = "INAPPROPRIATE|MISLEADING|SPAM|SAFETY|OTHER") String reason,
        @Size(max = 2000) String explanation) {}
