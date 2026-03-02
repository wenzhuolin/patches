package com.patches.plm.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ReviewSessionDetailResponse(
        Long sessionId,
        Long patchId,
        String mode,
        String meetingTool,
        String meetingUrl,
        Double quorumRequired,
        Double approveRateRequired,
        String status,
        String conclusion,
        Double approveRate,
        Long totalVotes,
        Long passVotes,
        Long rejectVotes,
        Long abstainVotes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ReviewVoteDetailResponse> votes
) {
}
