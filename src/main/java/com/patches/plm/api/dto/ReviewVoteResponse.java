package com.patches.plm.api.dto;

public record ReviewVoteResponse(
        Long sessionId,
        Long voterId,
        String vote,
        String comment
) {
}
