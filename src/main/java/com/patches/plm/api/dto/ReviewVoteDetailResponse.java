package com.patches.plm.api.dto;

import java.time.OffsetDateTime;

public record ReviewVoteDetailResponse(
        Long voteId,
        Long voterId,
        String vote,
        String comment,
        OffsetDateTime votedAt
) {
}
