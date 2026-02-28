package com.patches.plm.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewVoteRequest(
        @NotBlank String vote,
        String comment
) {
}
