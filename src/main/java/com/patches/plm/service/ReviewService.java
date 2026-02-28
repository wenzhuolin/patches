package com.patches.plm.service;

import com.patches.plm.api.dto.*;
import com.patches.plm.common.ErrorCode;
import com.patches.plm.common.exception.BusinessException;
import com.patches.plm.domain.entity.ReviewSessionEntity;
import com.patches.plm.domain.entity.ReviewVoteEntity;
import com.patches.plm.domain.repository.PatchRepository;
import com.patches.plm.domain.repository.ReviewSessionRepository;
import com.patches.plm.domain.repository.ReviewVoteRepository;
import com.patches.plm.web.RequestContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class ReviewService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final PatchRepository patchRepository;
    private final AuditLogService auditLogService;

    public ReviewService(ReviewSessionRepository reviewSessionRepository,
                         ReviewVoteRepository reviewVoteRepository,
                         PatchRepository patchRepository,
                         AuditLogService auditLogService) {
        this.reviewSessionRepository = reviewSessionRepository;
        this.reviewVoteRepository = reviewVoteRepository;
        this.patchRepository = patchRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReviewSessionResponse createSession(Long tenantId, ReviewSessionCreateRequest request, RequestContext context) {
        patchRepository.findByIdAndTenantId(request.patchId(), tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "补丁不存在"));
        ReviewSessionEntity entity = new ReviewSessionEntity();
        entity.setTenantId(tenantId);
        entity.setPatchId(request.patchId());
        entity.setMode(request.mode().toUpperCase(Locale.ROOT));
        entity.setMeetingTool(request.meetingTool());
        entity.setMeetingUrl(request.meetingUrl());
        entity.setQuorumRequired(request.quorumRequired());
        entity.setApproveRateRequired(request.approveRateRequired());
        entity.setStatus("OPEN");
        entity.setConclusion("PENDING");
        entity.setCreatedBy(context.userId());
        entity.setUpdatedBy(context.userId());
        ReviewSessionEntity saved = reviewSessionRepository.save(entity);
        auditLogService.log("REVIEW_SESSION", saved.getId(), "CREATE", null, saved, context);
        return new ReviewSessionResponse(saved.getId(), saved.getPatchId(), saved.getMode(), saved.getStatus(), saved.getConclusion(), 0.0);
    }

    @Transactional
    public ReviewVoteResponse vote(Long tenantId, Long sessionId, ReviewVoteRequest request, RequestContext context) {
        if (!(context.roles().contains("REVIEWER") || context.roles().contains("LINE_ADMIN") || context.roles().contains("SUPER_ADMIN"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色无权投票");
        }

        ReviewSessionEntity session = reviewSessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "评审会不存在"));
        if ("CLOSED".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评审会已关闭");
        }

        String vote = request.vote().toUpperCase(Locale.ROOT);
        if (!("PASS".equals(vote) || "REJECT".equals(vote) || "ABSTAIN".equals(vote))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "vote仅支持 PASS/REJECT/ABSTAIN");
        }

        ReviewVoteEntity voteEntity = reviewVoteRepository.findBySessionIdAndVoterId(sessionId, context.userId())
                .orElseGet(ReviewVoteEntity::new);
        voteEntity.setSessionId(sessionId);
        voteEntity.setVoterId(context.userId());
        voteEntity.setVote(vote);
        voteEntity.setComment(request.comment());
        reviewVoteRepository.save(voteEntity);

        long total = reviewVoteRepository.countBySessionId(sessionId);
        long pass = reviewVoteRepository.countBySessionIdAndVote(sessionId, "PASS");
        long reject = reviewVoteRepository.countBySessionIdAndVote(sessionId, "REJECT");
        double approveRate = total == 0 ? 0 : (pass * 100.0 / total);

        double threshold = session.getApproveRateRequired() == null ? 100.0 : session.getApproveRateRequired();
        if (approveRate >= threshold) {
            session.setConclusion("PASS");
            session.setStatus("CLOSED");
        } else if (reject > 0 && (reject * 100.0 / total) > (100 - threshold)) {
            session.setConclusion("REJECT");
            session.setStatus("CLOSED");
        }
        session.setUpdatedBy(context.userId());
        reviewSessionRepository.save(session);
        auditLogService.log("REVIEW_SESSION", session.getId(), "VOTE", null,
                new ReviewSessionResponse(session.getId(), session.getPatchId(), session.getMode(),
                        session.getStatus(), session.getConclusion(), approveRate), context);
        return new ReviewVoteResponse(sessionId, context.userId(), vote, request.comment());
    }
}
